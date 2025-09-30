package com.avaje.jdk.realworld.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LmdbAccountServiceTest {

  @Test
  void testHighVolumeOfTransactions() {
      final Instant startTime = Instant.now();
    final int numberOfTransactions = 10_000;
    final long mapSize = 100 * 1024 * 1024; // 100MB

    try (final var service = new LmdbAccountService(mapSize);
        final var executor = Executors.newVirtualThreadPerTaskExecutor()) {

      final List<Future<Account>> futures = new ArrayList<>();
      for (int i = 0; i < numberOfTransactions; i++) {
        final int id = i;
        futures.add(
            executor.submit(
                () -> {
                  final var account =
                      service.save(
                          new Account(
                              "test" + id + "@test.com", "test" + id, "test" + id, "bio", "image", null));
                  final var found = service.findById(account.getId());
                  assertNotNull(found);
                  assertEquals(account.getEmail(), found.getEmail());
                  return found;
                }));
      }

      assertEquals(numberOfTransactions, futures.size());
      for (Future<Account> future : futures) {
        try {
          assertNotNull(future.get());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
        System.out.printf("Execution time: %d ms%n", Instant.now().toEpochMilli() - startTime.toEpochMilli());
    }
  }
}
