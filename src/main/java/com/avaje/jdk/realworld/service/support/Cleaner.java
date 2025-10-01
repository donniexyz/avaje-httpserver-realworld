package com.avaje.jdk.realworld.service.support;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Utility to explicitly clean up DirectByteBuffers.
 * This is a workaround to suggest to the GC that the off-heap memory can be released.
 */
@Slf4j
public final class Cleaner {

  private static final Method GET_CLEANER_METHOD;
  private static final Method CLEAN_METHOD;

  static {
    Method getCleaner = null;
    Method clean = null;
    try {
      // We need a sample DirectByteBuffer to get its class for reflection
      ByteBuffer sample = ByteBuffer.allocateDirect(1);
      getCleaner = sample.getClass().getMethod("cleaner");
      getCleaner.setAccessible(true);
      Object cleanerInstance = getCleaner.invoke(sample);
      if (cleanerInstance != null) {
        clean = cleanerInstance.getClass().getMethod("clean");
      }
      log.info("DirectByteBuffer cleaner is available via reflection.");
    } catch (Exception e) {
      log.warn("DirectByteBuffer cleaner is not available. Off-heap memory release will be delayed. Reason: {}", e.toString());
    }
    GET_CLEANER_METHOD = getCleaner;
    CLEAN_METHOD = clean;
  }

  public static void clean(ByteBuffer buffer) {
    if (buffer != null && buffer.isDirect() && GET_CLEANER_METHOD != null && CLEAN_METHOD != null) {
      try {
        Object cleaner = GET_CLEANER_METHOD.invoke(buffer);
        if (cleaner != null) {
          CLEAN_METHOD.invoke(cleaner);
        }
      } catch (Exception e) {
        // Should not happen if static init succeeded, but log just in case.
        log.error("Unexpected error while cleaning direct buffer", e);
      }
    }
  }
}