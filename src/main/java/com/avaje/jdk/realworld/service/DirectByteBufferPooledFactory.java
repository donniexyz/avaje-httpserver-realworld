package com.avaje.jdk.realworld.service;

import com.google.flatbuffers.FlatBufferBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.nio.ByteBuffer;

@Slf4j
public class DirectByteBufferPooledFactory extends FlatBufferBuilder.ByteBufferFactory {

    private int maxBufferSize = 4 * 1024;

    private int timeoutMillis = 1000;

    public static final DirectByteBufferPooledFactory DEFAULT_INSTANCE = new DirectByteBufferPooledFactory();
    private final GenericObjectPoolConfig<ByteBuffer> DEFAULT_POOL_CONFIG;

    private final GenericObjectPool<ByteBuffer> bufferPool;

    public DirectByteBufferPooledFactory() {
        DEFAULT_POOL_CONFIG = new GenericObjectPoolConfig<>();
        DEFAULT_POOL_CONFIG.setMinIdle(500);
        DEFAULT_POOL_CONFIG.setMaxTotal(500000);

        bufferPool = new GenericObjectPool<ByteBuffer>(
                new ByteBufferObjectFactory(maxBufferSize, true),
                DEFAULT_POOL_CONFIG);
    }

    public DirectByteBufferPooledFactory(int maxBufferSize, int timeoutMillis) {
        DEFAULT_POOL_CONFIG = new GenericObjectPoolConfig<>();
        DEFAULT_POOL_CONFIG.setMinIdle(50);
        DEFAULT_POOL_CONFIG.setMaxTotal(500);

        this.maxBufferSize = maxBufferSize;
        this.timeoutMillis = timeoutMillis;
        bufferPool = new GenericObjectPool<ByteBuffer>(
                new ByteBufferObjectFactory(maxBufferSize, true),
                DEFAULT_POOL_CONFIG);
        try {
            bufferPool.preparePool();
        } catch (Exception e) {
            // do nothing
        }
    }

    @Override
    public ByteBuffer newByteBuffer(int capacity) {
        try {
            if (capacity > maxBufferSize) log.warn("capacity req exceed MAX: {}", capacity);
            return bufferPool.borrowObject(timeoutMillis);
        } catch (Exception e) {
            log.error("newByteBuffer exception", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void releaseByteBuffer(ByteBuffer bb) {
        bufferPool.returnObject(bb);
    }
}
