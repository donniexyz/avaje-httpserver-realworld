package com.avaje.jdk.realworld.pool.stormpot;

import stormpot.Pool;
import stormpot.Timeout;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AccountKeyVal {

    public static final AccountKeyVal DEFAULT = new AccountKeyVal(511, 1024, Duration.ofSeconds(1));

    private final int keySize;
    private final int valueSize;
    private final Pool<PollableByteBuffer> keyPool;
    private final Pool<PollableByteBuffer> valuePool;
    private final Duration timeoutDuration;
    private final Timeout timeout;

    public AccountKeyVal(int keySize, int valueSize, Duration timeoutDuration) {
        this.keySize = keySize;
        this.valueSize = valueSize;
        this.timeoutDuration = timeoutDuration;
        this.timeout = new Timeout(this.timeoutDuration);
        this.keyPool = Pool.from(new AllocatorByteBuffer(this.keySize)).build();
        this.valuePool = Pool.from(new AllocatorByteBuffer(this.valueSize)).build();
    }

    public <T> T whenKey(Function<ByteBuffer, T> keyObtainedHandler,
                         Function<Throwable, T> exceptionHandler) {
        try (PollableByteBuffer pollableByteBuffer = keyPool.claim(timeout)) {
            return keyObtainedHandler.apply(pollableByteBuffer.getObject());
        } catch (Throwable e) {
            return exceptionHandler.apply(e);
        }
    }

    public <T> T whenValue(Function<ByteBuffer, T> valueObtainedHandler,
                           Function<Throwable, T> exceptionHandler) {
        try (PollableByteBuffer pollableByteBuffer = valuePool.claim(timeout)) {
            return valueObtainedHandler.apply(pollableByteBuffer.getObject());
        } catch (Throwable e) {
            return exceptionHandler.apply(e);
        }
    }

    public <T> T whenKeyValue(BiFunction<ByteBuffer, ByteBuffer, T> keyValObtainedHandler,
                              Function<Throwable, T> exceptionHandler) {
        try (
                PollableByteBuffer keyByteBuffer = keyPool.claim(timeout);
                PollableByteBuffer valueByteBuffer = valuePool.claim(timeout)) {

            return keyValObtainedHandler.apply(keyByteBuffer.getObject(), valueByteBuffer.getObject());

        } catch (Throwable e) {
            return exceptionHandler.apply(e);
        }
    }


}
