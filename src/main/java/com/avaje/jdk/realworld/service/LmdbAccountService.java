package com.avaje.jdk.realworld.service;

import com.avaje.jdk.realworld.pool.stormpot.AccountKeyVal;
import jakarta.inject.Singleton;
import org.lmdbjava.Dbi;
import org.lmdbjava.Env;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.lmdbjava.DbiFlags.MDB_CREATE;

@Singleton
public class LmdbAccountService {

    private static final String DB_NAME = "ACC_BAL";
    private static final Integer ACC_KEY_SIZE = 256;
    private static final Integer ACC_VALUE_SIZE = 1024;

    Dbi<ByteBuffer> accBalDbi;

    public Integer initialize() {

        // We need a storage directory first.
        // The path cannot be on a remote file system.
        final File path;
        try {
            path = File.createTempFile("$LMDB_MY_DB", ".lmdb.tmp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // We always need an Env. An Env owns a physical on-disk storage file. One
        // Env can store many different databases (ie sorted maps).
        final Env<ByteBuffer> env =
                Env.create()
                        // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                        .setMapSize(1_048_576)
                        // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                        .setMaxDbs(1)
                        // Now let's open the Env. The same path can be concurrently opened and
                        // used in different processes, but do not open the same path twice in
                        // the same process at the same time.
                        .open(path);

        // We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
        // MDB_CREATE flag causes the DB to be created if it doesn't already exist.
        accBalDbi = env.openDbi(DB_NAME, MDB_CREATE);

        return 1;
    }

    public boolean demo() {
        // We want to store some data, so we will need a direct ByteBuffer.
        // Note that LMDB keys cannot exceed maxKeySize bytes (511 bytes by default).
        // Values can be larger.

        Boolean result = AccountKeyVal.DEFAULT.whenKeyValue(
                (keyBufer, valueBuffer) -> {
                    keyBufer.put("greeting".getBytes(UTF_8)).flip();
                    valueBuffer.put("Hello world".getBytes(UTF_8)).flip();

                    final int valSize = valueBuffer.remaining();

                    // Now store it. Dbi.put() internally begins and commits a transaction (Txn).
                    accBalDbi.put(keyBufer, valueBuffer);

                    return true;
                },
                (throwables) -> {
                    return false;
                }
        );
        return result;

    }

}
