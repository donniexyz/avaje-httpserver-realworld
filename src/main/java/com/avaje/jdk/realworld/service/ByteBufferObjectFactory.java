package com.avaje.jdk.realworld.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
@RequiredArgsConstructor
@Slf4j
public class ByteBufferObjectFactory extends BasePooledObjectFactory<ByteBuffer> {

    private final int maxBufferSize;
    private boolean littleEndian;
    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public ByteBuffer create() throws Exception {
        int cnt = count.incrementAndGet();
        if (cnt % 1000 == 0) log.info("crete: {}", cnt);
        return ByteBuffer.allocateDirect(maxBufferSize).order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }

    @Override
    public PooledObject<ByteBuffer> wrap(ByteBuffer obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<ByteBuffer> p, DestroyMode destroyMode) throws Exception {
        p.deallocate();
    }
}
