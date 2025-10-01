package com.avaje.jdk.realworld.service;

import com.google.flatbuffers.FlatBufferBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A ByteBufferFactory that manages a single large DirectByteBuffer (an "arena")
 * and serves fixed-size slices from it. This is highly efficient for use cases
 * like fixed-size database keys (e.g., UUIDs).
 *
 * <p><b>How it works:</b>
 * 1. An initial direct ByteBuffer (arena) is allocated at creation.
 * 2. This arena is pre-sliced into many smaller, fixed-size ByteBuffers (slices).
 * 3. These slices are stored in a blocking queue, which acts as the object pool.
 * 4. If the pool becomes empty, it can dynamically grow by allocating additional arenas.
 * 5. `newByteBuffer` takes a slice from the queue, triggering growth if necessary.
 * 5. `releaseByteBuffer` returns a slice to the queue.
 *
 * <p><b>Benefits:</b>
 * - Reduces system calls for memory allocation, improving performance.
 * - Improves memory locality, which can lead to fewer CPU cache misses.
 * - Virtually zero allocation overhead after initial setup.
 */
@Slf4j
public final class ArenaByteBufferFactory extends FlatBufferBuilder.ByteBufferFactory {

  /** Holds all the slices from all arenas. */
  private final BlockingQueue<ByteBuffer> pool;
  /** Tracks all the master arena buffers for management. */
  private final List<ByteBuffer> arenas = new CopyOnWriteArrayList<>();
  /** Lock to ensure only one thread can grow the arena at a time. */
  private final ReentrantLock growLock = new ReentrantLock();
  /** Tracks the total capacity of all allocated arenas. */
  private final AtomicLong currentTotalCapacity = new AtomicLong(0);

  private final int sliceCapacity;
  private final int growCapacity;
  private final int timeoutMillis;
  private final long maxCapacity;

  /**
   * Creates an Arena allocator.
   *
   * @param initialCapacity The total size of the memory arena to allocate (e.g., 1MB).
   * @param growCapacity The size of each new arena to allocate when the pool is exhausted. If <= 0, no growth will occur.
   * @param sliceCapacity The fixed size of each slice to be served (e.g., 64 bytes for a UUID key).
   * @param timeoutMillis The time to wait to borrow a slice before failing.
   * @param maxCapacity The maximum total memory this arena is allowed to allocate.
   */
  public ArenaByteBufferFactory(int initialCapacity, int growCapacity, int sliceCapacity, int timeoutMillis, long maxCapacity) {
    if (initialCapacity < sliceCapacity) {
      throw new IllegalArgumentException("Total capacity must be >= slice capacity");
    }
    if (growCapacity > 0 && growCapacity < sliceCapacity) { // Only validate if growth is enabled
      throw new IllegalArgumentException("Grow capacity must be >= slice capacity");
    }
    if (maxCapacity < initialCapacity) {
      throw new IllegalArgumentException("Max capacity must be >= initial total capacity");
    }
    this.sliceCapacity = sliceCapacity;
    this.growCapacity = growCapacity;
    this.timeoutMillis = timeoutMillis;
    this.pool = new LinkedBlockingQueue<>();
    this.maxCapacity = maxCapacity;

    // Allocate and slice the initial arena
    addNewArena(initialCapacity);
  }

  @Override
  public ByteBuffer newByteBuffer(int capacity) {
    // This factory only supports a fixed capacity
    if (capacity > this.sliceCapacity) {
      throw new IllegalArgumentException("Requested capacity " + capacity + " exceeds slice capacity " + this.sliceCapacity);
    }

    try {
      ByteBuffer buffer = pool.poll(timeoutMillis, TimeUnit.MILLISECONDS);
      if (buffer != null) {
        return buffer;
      }

      // Timeout occurred, pool is likely empty. Attempt to grow if enabled.
      if (growCapacity <= 0) {
        // Growth is disabled, so we can't get a buffer.
        throw new RuntimeException("Could not borrow a ByteBuffer from the arena. Pool is empty and growth is disabled.");
      }

      growLock.lock();
      try {
        // Double-check if another thread grew the pool while we were waiting for the lock
        buffer = pool.poll();
        if (buffer != null) {
          return buffer;
        }
        // We have the lock and the pool is still empty. Check if we can grow.
        if (currentTotalCapacity.get() + growCapacity > maxCapacity) {
          log.warn("Cannot grow arena. Max capacity ({}) would be exceeded. Current capacity: {}", maxCapacity, currentTotalCapacity.get());
        } else {
          // We can grow.
          addNewArena(this.growCapacity);
        }
      } finally {
        growLock.unlock();
      }

      // After growing, try one more time to get a buffer.
      // This could still fail if the grow operation was small and all new buffers were taken.
      buffer = pool.poll(timeoutMillis, TimeUnit.MILLISECONDS);
      if (buffer != null) {
        return buffer;
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for a ByteBuffer from the arena", e);
    }

    // If we reach here, it means the pool was empty, we grew it, and it was still empty after a timeout.
    // This indicates extreme contention or a very small growCapacity.
    throw new RuntimeException("Could not borrow a ByteBuffer from the arena even after attempting to grow.");
  }

  private void addNewArena(int arenaCapacity) {
    log.info("Allocating new direct memory arena of {} bytes, with slice capacity of {} bytes", arenaCapacity, sliceCapacity);
    ByteBuffer newArena = ByteBuffer.allocateDirect(arenaCapacity);
    currentTotalCapacity.addAndGet(arenaCapacity);
    arenas.add(newArena);

    int numSlices = arenaCapacity / sliceCapacity;
    for (int i = 0; i < numSlices; i++) {
      int position = i * sliceCapacity;
      ByteBuffer slice = newArena.duplicate()
          .position(position)
          .limit(position + sliceCapacity)
          .slice();
      pool.add(slice);
    }
    log.info("Arena grown. Added {} new slices. Pool size: {}, Total capacity: {} bytes", numSlices, pool.size(), currentTotalCapacity.get());
  }

  @Override
  public void releaseByteBuffer(ByteBuffer bb) {
    bb.clear(); // Reset position and limit for next use
    // Defensive check: ensure the buffer being returned is a valid slice from this arena.
    if (bb.capacity() != this.sliceCapacity) {
      log.error("Attempted to return a ByteBuffer with incorrect capacity ({}) to the arena. Expected {}. Buffer will not be returned to the pool.", bb.capacity(), this.sliceCapacity);
      return;
    }

    if (!pool.offer(bb)) {
      log.warn("Failed to return ByteBuffer to the arena pool, it might be full or an invalid buffer was returned.");
    }
  }

}