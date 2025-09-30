package com.avaje.jdk.realworld.service;

import com.google.flatbuffers.FlatBufferBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DirectByteBufferPooledFactory extends FlatBufferBuilder.ByteBufferFactory {

    private int maxBufferSize = 4 * 1024;

    private int timeoutMillis = 1000;

    public static final DirectByteBufferPooledFactory DEFAULT_INSTANCE = new DirectByteBufferPooledFactory();

    private final GenericObjectPool<ByteBuffer> bufferPool;

    public DirectByteBufferPooledFactory() {
        GenericObjectPoolConfig<ByteBuffer> DEFAULT_POOL_CONFIG = getPoolConfig();

        bufferPool = new GenericObjectPool<ByteBuffer>(
                new ByteBufferObjectFactory(maxBufferSize, true),
                DEFAULT_POOL_CONFIG);
    }

    public DirectByteBufferPooledFactory(int maxBufferSize, int timeoutMillis) {
        GenericObjectPoolConfig<ByteBuffer> DEFAULT_POOL_CONFIG = getPoolConfig();

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

    @Getter
    private final AtomicInteger relcnt = new AtomicInteger();

    @Override
    public void releaseByteBuffer(ByteBuffer bb) {
        relcnt.getAndIncrement();
        bufferPool.returnObject(bb);
    }

    // -----------------------------------------------------------

    private static @NotNull GenericObjectPoolConfig<ByteBuffer> getPoolConfig() {
        GenericObjectPoolConfig<ByteBuffer> DEFAULT_POOL_CONFIG = new GenericObjectPoolConfig<>();
        DEFAULT_POOL_CONFIG.setMinIdle(2000);
        DEFAULT_POOL_CONFIG.setMaxTotal(50000);
        return DEFAULT_POOL_CONFIG;
    }
}
