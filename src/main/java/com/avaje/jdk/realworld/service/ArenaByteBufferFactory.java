package com.avaje.jdk.realworld.service;

import com.google.flatbuffers.FlatBufferBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A ByteBufferFactory that manages a single large DirectByteBuffer (an "arena")
 * and serves fixed-size slices from it. This is highly efficient for use cases
 * like fixed-size database keys (e.g., UUIDs).
 *
 * <p><b>How it works:</b>
 * 1. A large direct ByteBuffer is allocated once at creation.
 * 2. This arena is pre-sliced into many smaller, fixed-size ByteBuffers.
 * 3. These slices are stored in a blocking queue, which acts as the object pool.
 * 4. `newByteBuffer` takes a slice from the queue.
 * 5. `releaseByteBuffer` returns a slice to the queue.
 *
 * <p><b>Benefits:</b>
 * - Reduces system calls for memory allocation, improving performance.
 * - Improves memory locality, which can lead to fewer CPU cache misses.
 * - Virtually zero allocation overhead after initial setup.
 */
@Slf4j
public final class ArenaByteBufferFactory extends FlatBufferBuilder.ByteBufferFactory {

  private final ByteBuffer arena;
  private final BlockingQueue<ByteBuffer> pool;
  private final int sliceCapacity;
  private final int timeoutMillis;

  /**
   * Creates an Arena allocator.
   *
   * @param totalCapacity The total size of the memory arena to allocate (e.g., 1MB).
   * @param sliceCapacity The fixed size of each slice to be served (e.g., 64 bytes for a UUID key).
   * @param timeoutMillis The time to wait to borrow a slice before failing.
   */
  public ArenaByteBufferFactory(int totalCapacity, int sliceCapacity, int timeoutMillis) {
    if (totalCapacity < sliceCapacity) {
      throw new IllegalArgumentException("Total capacity must be >= slice capacity");
    }
    this.sliceCapacity = sliceCapacity;
    this.timeoutMillis = timeoutMillis;

    log.info("Allocating a direct memory arena of {} bytes, with slice capacity of {} bytes", totalCapacity, sliceCapacity);
    this.arena = ByteBuffer.allocateDirect(totalCapacity);

    int numSlices = totalCapacity / sliceCapacity;
    this.pool = new ArrayBlockingQueue<>(numSlices);

    // Pre-slice the arena and fill the pool with buffer views
    for (int i = 0; i < numSlices; i++) {
      int position = i * sliceCapacity;
      // Create a view of the arena for this slice
      ByteBuffer slice = arena.duplicate()
        .position(position)
        .limit(position + sliceCapacity)
        .slice(); // .slice() makes its position 0 and limit=capacity
      pool.add(slice);
    }
    log.info("Arena pre-sliced into {} available buffers.", numSlices);
  }

  @Override
  public ByteBuffer newByteBuffer(int capacity) {
    // This factory only supports a fixed capacity
    if (capacity > this.sliceCapacity) {
      throw new IllegalArgumentException("Requested capacity " + capacity + " exceeds slice capacity " + this.sliceCapacity);
    }
    try {
      return pool.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for a ByteBuffer from the arena", e);
    }
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