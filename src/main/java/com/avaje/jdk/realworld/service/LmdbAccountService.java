package com.avaje.jdk.realworld.service;

import com.avaje.jdk.realworld.models.flat.Account;
import com.google.flatbuffers.FlatBufferBuilder;
import jakarta.inject.Singleton;
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
public class LmdbAccountService implements AutoCloseable {

  private static final String DB_NAME = "ACCOUNTS";

  private final Env<ByteBuffer> env;
  private final Dbi<ByteBuffer> dbi;
  private final Path path;

  public LmdbAccountService() {
    try {
      this.path = Files.createTempDirectory("lmdb-accounts");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.env = Env.create().setMapSize(10_485_760).setMaxDbs(1).open(path.toFile());
    this.dbi = env.openDbi(DB_NAME, MDB_CREATE);
  }

  public com.avaje.jdk.realworld.service.Account save(
      com.avaje.jdk.realworld.service.Account account) {
    final String id = UUID.randomUUID().toString();
    account.setId(id);

    final FlatBufferBuilder builder = new FlatBufferBuilder(1024);

    final int email = builder.createString(account.getEmail());
    final int username = builder.createString(account.getUsername());
    final int password = builder.createString(account.getPassword());
    final int bio = builder.createString(account.getBio());
    final int image = builder.createString(account.getImage());
    final int idOffset = builder.createString(id);

    Account.startAccount(builder);
    Account.addId(builder, idOffset);
    Account.addEmail(builder, email);
    Account.addUsername(builder, username);
    Account.addPassword(builder, password);
    Account.addBio(builder, bio);
    Account.addImage(builder, image);
    final int accountOffset = Account.endAccount(builder);

    builder.finish(accountOffset);

    final ByteBuffer value = builder.dataBuffer();

    final ByteBuffer key = ByteBuffer.allocateDirect(env.getMaxKeySize());
    key.put(id.getBytes(UTF_8)).flip();

    dbi.put(key, value);
    return account;
  }

  public com.avaje.jdk.realworld.service.Account findById(String id) {
    final ByteBuffer key = ByteBuffer.allocateDirect(env.getMaxKeySize());
    key.put(id.getBytes(UTF_8)).flip();

    try (Txn<ByteBuffer> txn = env.txnRead()) {
      final ByteBuffer foundValue = dbi.get(txn, key);
      if (foundValue == null) {
        return null;
      }

      final Account flatAccount = Account.getRootAsAccount(foundValue);
      return new com.avaje.jdk.realworld.service.Account(
          flatAccount.email(),
          flatAccount.username(),
          flatAccount.password(),
          flatAccount.bio(),
          flatAccount.image(),
          flatAccount.id());
    }
  }

  @Override
  public void close() throws Exception {
    env.close();
    Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }
}
