package com.brandPitara.sfs.migration.wordpress.media;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Realistic in-memory {@link WordPressMigrationMediaStorage} for hermetic tests - stores actual
 * byte arrays (never fakes them), computes real SHA-256 checksums from those bytes, and asserts
 * no database transaction is active during any call (the whole point of the transaction-boundary
 * requirement this milestone is built around). Never touches real AWS.
 */
public class InMemoryWordPressMigrationMediaStorage implements WordPressMigrationMediaStorage {

    public record StoredObject(byte[] bytes, String contentType, String sha256Hex) {
    }

    private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();
    private final AtomicInteger storeInvocations = new AtomicInteger();
    private final AtomicInteger headInvocations = new AtomicInteger();
    private RuntimeException nextStoreFailure;
    private RuntimeException nextHeadFailure;
    private boolean corruptNextStoredChecksum;
    private Runnable onNextHead;

    @Override
    public void store(String bucket, String key, Path localFile, String contentType) {
        assertNoActiveTransaction("store");
        storeInvocations.incrementAndGet();
        if (nextStoreFailure != null) {
            RuntimeException failure = nextStoreFailure;
            nextStoreFailure = null;
            throw failure;
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(localFile);
        } catch (IOException e) {
            throw new RuntimeException("Test storage could not read " + localFile, e);
        }
        String composite = compositeKey(bucket, key);
        String sha256 = sha256Hex(bytes);
        StoredObject existing = objects.get(composite);
        if (existing != null) {
            if (!existing.sha256Hex().equals(sha256)) {
                throw new RuntimeException(
                        "Precondition Failed: object already exists at " + composite + " with different content "
                                + "(simulates S3's If-None-Match: * rejection).");
            }
            return;
        }
        String recordedChecksum = corruptNextStoredChecksum ? "0000000000000000000000000000000000000000000000000000000000000000" : sha256;
        corruptNextStoredChecksum = false;
        objects.put(composite, new StoredObject(bytes, contentType, recordedChecksum));
    }

    @Override
    public WordPressStoredObjectMetadata head(String bucket, String key) {
        assertNoActiveTransaction("head");
        headInvocations.incrementAndGet();
        if (onNextHead != null) {
            Runnable hook = onNextHead;
            onNextHead = null;
            hook.run();
        }
        if (nextHeadFailure != null) {
            RuntimeException failure = nextHeadFailure;
            nextHeadFailure = null;
            throw failure;
        }
        StoredObject stored = objects.get(compositeKey(bucket, key));
        if (stored == null) {
            throw new WordPressMediaStorageObjectNotFoundException(bucket, key);
        }
        return new WordPressStoredObjectMetadata(stored.bytes().length, stored.contentType(), stored.sha256Hex());
    }

    @Override
    public boolean exists(String bucket, String key) {
        assertNoActiveTransaction("exists");
        return objects.containsKey(compositeKey(bucket, key));
    }

    @Override
    public void deleteOrphan(String bucket, String key) {
        assertNoActiveTransaction("deleteOrphan");
        objects.remove(compositeKey(bucket, key));
    }

    // ---- test-only inspection / control surface ----

    public byte[] storedBytes(String bucket, String key) {
        StoredObject stored = objects.get(compositeKey(bucket, key));
        return stored == null ? null : stored.bytes();
    }

    public boolean hasObject(String bucket, String key) {
        return objects.containsKey(compositeKey(bucket, key));
    }

    public int objectCount() {
        return objects.size();
    }

    public int storeInvocationCount() {
        return storeInvocations.get();
    }

    public int headInvocationCount() {
        return headInvocations.get();
    }

    public void failNextStore(RuntimeException failure) {
        this.nextStoreFailure = failure;
    }

    public void failNextHead(RuntimeException failure) {
        this.nextHeadFailure = failure;
    }

    /**
     * Runs once, immediately before the next {@link #head} call does anything else (including a
     * forced {@link #failNextHead} failure) - lets a test deterministically simulate a concurrent
     * import landing (e.g. finalizing a competing {@code CmsMediaAssetEntity} at the same
     * content-addressed key) at the exact interleaving point a real race would occur at, without
     * real threads.
     */
    public void runBeforeNextHead(Runnable hook) {
        this.onNextHead = hook;
    }

    public void corruptNextStoredChecksum() {
        this.corruptNextStoredChecksum = true;
    }

    /** Seeds an object directly, bypassing store() - used to simulate a pre-existing/out-of-band object. */
    public void seed(String bucket, String key, byte[] bytes, String contentType) {
        objects.put(compositeKey(bucket, key), new StoredObject(bytes, contentType, sha256Hex(bytes)));
    }

    public void removeQuietly(String bucket, String key) {
        objects.remove(compositeKey(bucket, key));
    }

    /** Clears all stored objects and invocation counters - call between tests sharing this bean. */
    public void reset() {
        objects.clear();
        storeInvocations.set(0);
        headInvocations.set(0);
        nextStoreFailure = null;
        nextHeadFailure = null;
        corruptNextStoredChecksum = false;
        onNextHead = null;
    }

    private void assertNoActiveTransaction(String operation) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new AssertionError(
                    "WordPressMigrationMediaStorage." + operation + "() was called while a database "
                            + "transaction was active - external storage I/O must never run inside a "
                            + "transaction (see the Hikari pool exhaustion this design avoids).");
        }
    }

    private String compositeKey(String bucket, String key) {
        return bucket + "/" + key;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
