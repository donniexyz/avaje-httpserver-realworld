package com.avaje.jdk.realworld.pool.stormpot;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import stormpot.Reallocator;
import stormpot.Slot;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@RequiredArgsConstructor
@AllArgsConstructor
public class AllocatorByteBuffer implements Reallocator<PollableByteBuffer> {

    private final int bufferSize;
    private boolean littleEndian;

    @Override
    public PollableByteBuffer allocate(Slot slot) {
        return new PollableByteBuffer(slot, ByteBuffer.allocateDirect(bufferSize).order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN));
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
