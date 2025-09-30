package com.avaje.jdk.realworld.service;

import com.google.flatbuffers.FlatBufferBuilder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DirectByteBufferFactory extends FlatBufferBuilder.ByteBufferFactory {

    public static final DirectByteBufferFactory INSTANCE = new DirectByteBufferFactory();

    @Override
    public ByteBuffer newByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }
}
