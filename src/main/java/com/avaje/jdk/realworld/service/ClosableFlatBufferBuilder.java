package com.avaje.jdk.realworld.service;

import com.google.flatbuffers.FlatBufferBuilder;

import java.io.Closeable;
import java.nio.ByteBuffer;

public class ClosableFlatBufferBuilder extends FlatBufferBuilder implements Closeable {

    private final DirectByteBufferPooledFactory bb_factory;

    public ClosableFlatBufferBuilder(int initial_size, DirectByteBufferPooledFactory bb_factory) {
        super(initial_size, bb_factory);
        this.bb_factory = bb_factory;
    }

    @Override
    public void close() {
        ByteBuffer buffer = dataBuffer();
        bb_factory.releaseByteBuffer(buffer);
    }

    public static class Unsafe extends ClosableFlatBufferBuilder {

        public Unsafe(int initial_size, DirectByteBufferPooledFactory bb_factory) {
            super(initial_size, bb_factory);
        }

        @Override
        public void close() {
            try {
                super.close();
            } catch (AssertionError error) {
                // ignore
            }
        }
    }
}