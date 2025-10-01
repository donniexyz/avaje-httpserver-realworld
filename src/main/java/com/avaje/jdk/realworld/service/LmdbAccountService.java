package com.avaje.jdk.realworld.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.lmdbjava.Dbi;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.lmdbjava.DbiFlags.MDB_CREATE;

@Singleton
@Slf4j
public class LmdbAccountService implements AutoCloseable {

    private static final String DB_NAME = "ACCOUNTS";
    private static final int BUFFER_SIZE = 1024;

    private final Env<ByteBuffer> env;
    private final Dbi<ByteBuffer> dbi;
    private final Path path;

    DirectByteBufferPooledFactory bbFactory;

    // Arena allocator specifically for our fixed-size UUID keys.
    // 1MB arena, with 64-byte slices (enough for a UUID string).
    // Will grow by 512KB if the initial pool is exhausted.
    // Max capacity is set to 4MB.
    ArenaByteBufferFactory keyFactory = new ArenaByteBufferFactory(1024 * 1024, 512 * 1024, 64, 10, 4 * 1024 * 1024);

    @Inject
    public LmdbAccountService() {
        this(10 * 1024 * 1024); // 10 MB
    }

    /**
     * Create with a given map size.
     */
    public LmdbAccountService(long mapSize) {
        try {
            this.path = Files.createTempDirectory("lmdb-accounts");
            log.info("lmdb-accounts path: {}", path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.env = Env.create().setMapSize(mapSize).setMaxDbs(1).open(path.toFile());
        this.dbi = env.openDbi(DB_NAME, MDB_CREATE);
        this.bbFactory = new DirectByteBufferPooledFactory(BUFFER_SIZE, 10);
// pooled      10k - SuccessExecution time: 11281 ms
// pooled      30k - SuccessExecution time: 24677 ms
// pooled - borrow-return 30k - Success11:00:44.520 [main] INFO com.avaje.jdk.realworld.service.LmdbAccountService - bbFactory Relcnt: 30000
//        Execution time: 24762 ms
// pooled - borrow-return 30k - key pooled - Execution time: 21745 ms

//        this.bbFactory = DirectByteBufferFactory.INSTANCE;
        //    final FlatBufferBuilder builder = new FlatBufferBuilder(1024, DirectByteBufferFactory.INSTANCE);
// not pooled  10k - SuccessExecution time: 11145 ms
// not pooled  30k - SuccessExecution time: 24921 ms

    }

    public AccountDTO save(AccountDTO accountDTO) {
        final String generatedId = UUID.randomUUID().toString();
        accountDTO.setId(generatedId);

        try (ClosableFlatBufferBuilder builder = new ClosableFlatBufferBuilder(BUFFER_SIZE, bbFactory)) {

            accountDTO.fill(builder);
            final ByteBuffer value = builder.dataBuffer();

            // Use try-with-resources to automatically manage the key buffer's lifecycle
            try (ManagedByteBuffer managedKey = keyFactory.borrow(64)) {
              final ByteBuffer key = managedKey.get();
              key.put(accountDTO.getId().getBytes(UTF_8)).flip();
              dbi.put(key, value);
              return accountDTO;
            }
        }
    }

    public AccountDTO findById(String id) {
      // Use try-with-resources to automatically manage the key buffer's lifecycle
      try (ManagedByteBuffer managedKey = keyFactory.borrow(64)) {
        final ByteBuffer key = managedKey.get();
        key.put(id.getBytes(UTF_8)).flip();

        try (Txn<ByteBuffer> txn = env.txnRead()) {
          final ByteBuffer foundValue = dbi.get(txn, key);
          if (foundValue == null) {
            return null;
          }

          final com.avaje.jdk.realworld.models.flat.Account flatAccount = com.avaje.jdk.realworld.models.flat.Account.getRootAsAccount(foundValue);
          return new AccountDTO(flatAccount.email(), flatAccount.username(), flatAccount.password(), flatAccount.bio(), flatAccount.image(), flatAccount.id());
        }
      }
    }

    @Override
    public void close() throws Exception {
        if (bbFactory instanceof DirectByteBufferPooledFactory asBb) {
            log.info("bbFactory Relcnt: {}", asBb.getRelcnt().get());
            log.info("bbFactory getBufferPool: {}", asBb.getBufferPool());
            asBb.getBufferPool().close();
        }
        keyFactory.close();
        env.close();
        Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
}
