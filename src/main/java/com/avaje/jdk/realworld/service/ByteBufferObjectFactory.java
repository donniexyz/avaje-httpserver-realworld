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
    private static final AtomicInteger cnt = new AtomicInteger(0);

    @Override
    public ByteBuffer create() throws Exception {
        int cnt = ByteBufferObjectFactory.cnt.incrementAndGet();
        if (cnt % 100 == 0) log.info("crete: {}", cnt);
        return ByteBuffer.allocateDirect(maxBufferSize).order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }

    @Override
    public PooledObject<ByteBuffer> wrap(ByteBuffer obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<ByteBuffer> p, DestroyMode destroyMode) throws Exception {
        super.destroyObject(p, destroyMode);
        p.deallocate();
    }
}
