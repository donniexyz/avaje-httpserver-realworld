package com.avaje.jdk.realworld.service;

import java.nio.ByteBuffer;

/**
 * A wrapper around a pooled ByteBuffer that implements AutoCloseable.
 * This allows the use of try-with-resources to ensure the buffer is
 * always returned to its factory.
 */
public final class ManagedByteBuffer implements AutoCloseable {

  private final ByteBuffer buffer;
  private final ArenaByteBufferFactory factory;

  ManagedByteBuffer(ByteBuffer buffer, ArenaByteBufferFactory factory) {
    this.buffer = buffer;
    this.factory = factory;
  }

  /**
   * @return The underlying ByteBuffer.
   */
  public ByteBuffer get() {
    return buffer;
  }

  @Override
  public void close() {
    factory.releaseByteBuffer(buffer);
  }
}