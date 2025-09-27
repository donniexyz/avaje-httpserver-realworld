package com.avaje.jdk.realworld.pool.stormpot;

import stormpot.Pooled;
import stormpot.Slot;

import java.nio.ByteBuffer;

public class PollableByteBuffer extends Pooled<ByteBuffer> {

    /**
     * Create a pooled object for the given slot and object to be pooled.
     *
     * @param slot   The slot an object is being allocated for.
     * @param object The object this pooled instance represents.
     */
    public PollableByteBuffer(Slot slot, ByteBuffer object) {
        super(slot, object);
    }

}
