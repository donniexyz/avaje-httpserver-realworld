package com.avaje.jdk.realworld.pool.stormpot;

import lombok.AllArgsConstructor;
import stormpot.Reallocator;
import stormpot.Slot;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class AllocatorByteBuffer implements Reallocator<PollableByteBuffer> {

    private final int bufferSize;

    @Override
    public PollableByteBuffer allocate(Slot slot) {
        return new PollableByteBuffer(slot, ByteBuffer.allocateDirect(bufferSize));
    }

    @Override
    public void deallocate(PollableByteBuffer poolable) {
        poolable.getObject().clear();
    }

    @Override
    public PollableByteBuffer reallocate(Slot slot, PollableByteBuffer poolable) {
        poolable.getObject().clear();
        return poolable;
    }
}
