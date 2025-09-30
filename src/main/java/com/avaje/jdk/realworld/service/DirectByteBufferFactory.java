package com.avaje.jdk.realworld.service;

import com.google.flatbuffers.FlatBufferBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DirectByteBufferFactory extends FlatBufferBuilder.ByteBufferFactory {

    public static final DirectByteBufferFactory INSTANCE = new DirectByteBufferFactory();

    private static final AtomicInteger cnt = new AtomicInteger();

    @Override
    public ByteBuffer newByteBuffer(int capacity) {
        if (cnt.incrementAndGet() % 100 == 0) log.info("capacity: {}", capacity);
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }
}
