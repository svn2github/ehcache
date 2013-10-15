/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.terracotta;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.DiskStorePathManager;
import net.sf.ehcache.util.PreferTCCLObjectInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A file will rotate on every write, so to never loose older values in case of a JVM crash
 *
 * @author Alex Snaps
 */
class RotatingSnapshotFile {

    private static final Logger LOG = LoggerFactory.getLogger(RotatingSnapshotFile.class);

    private static final String SUFFIX_OK = ".keySet";
    private static final String SUFFIX_PROGRESS = SUFFIX_OK + ".temp";
    private static final String SUFFIX_MOVE = SUFFIX_OK + ".old";

    private volatile boolean shutdownOnThreadInterrupted;
    private final String cacheName;

    private final Lock readLock;
    private final Lock writeLock;
    private final DiskStorePathManager diskStorePathManager;

    {
        ReadWriteLock rwl = new ReentrantReadWriteLock();
        readLock = rwl.readLock();
        writeLock = rwl.writeLock();
    }

    /**
     * Constructor
     *
     * @param cacheName  use as base name of the files
     */
    RotatingSnapshotFile(final DiskStorePathManager diskStorePathManager, final String cacheName) {
        this.diskStorePathManager = diskStorePathManager;
        this.cacheName = cacheName;
    }

    /**
     * Writes all values of the iterable to a new file and does the necessary clean up when done
     *
     * @param localKeys the iterable of entries to write to disk
     * @throws IOException If the underlying OutputStream do throw
     */
    void writeAll(final Iterable localKeys) throws IOException {
        writeLock.lock();
        long writtenKeys = 0;
        try {
            File inProgress = newSnapshotFile();

            cleanUp(inProgress);
            if (!inProgress.createNewFile()) {
                throw new AssertionError("The file '" + inProgress.getAbsolutePath() + "' exists already!");
            }

            final FileOutputStream fileOutputStream = new FileOutputStream(inProgress);
            final ObjectOutputStream oos = new ObjectOutputStream(fileOutputStream);

            try {
                for (Object localKey : localKeys) {
                    if (shutdownOnThreadInterrupted && Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    oos.writeObject(localKey);
                    ++writtenKeys;
                }
            } finally {
                fileOutputStream.close();
            }

            swapForOldWithNewSnapshot(inProgress);
        } finally {
            LOG.info("Did a snapshot of " + writtenKeys + " local keys");
            writeLock.unlock();
        }
    }

    /**
     * Reads all the keys from the file on disk, doing cleanup if required of previously unterminated file written to
     *
     * @param <T> the type of the each element
     * @return the Set of all entries in the latest uncorrupted file on disk
     * @throws IOException If the underlying FileInputStream does throw
     */
    <T> Set<T> readAll() throws IOException {

        cleanUp();

        readLock.lock();
        try {

            final File currentSnapshot = currentSnapshotFile();
            if (!currentSnapshot.exists()) {
                return Collections.emptySet();
            }

            final Set<T> values = new HashSet<T>();
            FileInputStream fis = new FileInputStream(currentSnapshot);
            try {
                ObjectInputStream ois = new PreferTCCLObjectInputStream(fis);
                boolean eof = false;
                while (!eof) {
                    try {
                        values.add((T)ois.readObject());
                    } catch (Exception e) {
                        if (e instanceof EOFException) {
                            eof = true;
                        }
                        // Ignore all other errors, and keep on trying to load keys
                    }
                }
                try {
                    ois.close();
                } catch (IOException e) {
                    LOG.error("Error closing ObjectInputStream", e);
                    closeAndDeleteAssociatedFileOnFailure(fis, currentSnapshot);
                }

            } catch (IOException e) {
                closeAndDeleteAssociatedFileOnFailure(fis, currentSnapshot);
            }
            return Collections.unmodifiableSet(values);
        } finally {
            readLock.unlock();
        }
    }

    private void cleanUp() {
        if (requiresCleanUp()) {
            writeLock.lock();
            try {
                cleanUp(newSnapshotFile());
            } finally {
                writeLock.unlock();
            }
        }
    }

    private void cleanUp(final File inProgress) {
        if (requiresCleanUp()) {
            final File dest = currentSnapshotFile();
            if (dest.exists() && !inProgress.delete()) {
                throw new RuntimeException("Couldn't cleanup old file " + inProgress.getAbsolutePath());
            } else {
                final File tempFile = tempSnapshotFile();
                if (tempFile.exists() && !tempFile.delete()) {
                    throw new RuntimeException("Couldn't cleanup temp file " + tempFile.getAbsolutePath());
                }
                if (inProgress.exists() && !inProgress.renameTo(dest)) {
                    throw new RuntimeException("Couldn't rename new snapshot: " + dest.getAbsolutePath());
                }
            }
        }
    }

    private boolean requiresCleanUp() {
        return newSnapshotFile().exists();
    }

    private void swapForOldWithNewSnapshot(final File inProgress) {
        File currentSnapshot = currentSnapshotFile();
        final File tempFile = tempSnapshotFile();
        if (currentSnapshot.exists() && !currentSnapshot.renameTo(tempFile)) {
            throw new RuntimeException("Couldn't rename previous snapshot: " + currentSnapshot.getAbsolutePath());
        }
        if (!inProgress.renameTo(currentSnapshot)) {
            throw new RuntimeException("Couldn't rename new snapshot: " + currentSnapshot.getAbsolutePath());
        }
        if (tempFile.exists() && !tempFile.delete()) {
            throw new RuntimeException("Couldn't delete temp file " + tempFile.getAbsolutePath());
        }
    }

    /**
     * Creates a File representing the uncorrupted file on disk
     *
     * @return the file to read from
     */
    File currentSnapshotFile() {
        return diskStorePathManager.getFile(cacheName, SUFFIX_OK);
    }

    /**
     * Creates a File representing the one to write new entries to
     *
     * @return the File to write to
     */
    File newSnapshotFile() {
        return diskStorePathManager.getFile(cacheName, SUFFIX_PROGRESS);
    }

    /**
     * Creates a File representing the old uncorrupted file, when the new one has successfully been written to disk
     *
     * @return the File representing the previous successful snapshot (temp file to be deleted)
     */
    File tempSnapshotFile() {
        return diskStorePathManager.getFile(cacheName, SUFFIX_MOVE);
    }

    /**
     * Whether to shutdown as soon as the writer Thread is interrupted, or to let all keys be written to disk first
     *
     * @param shutdownOnThreadInterrupted true, if shutdown needs to happen in the middle of a write
     */
    void setShutdownOnThreadInterrupted(final boolean shutdownOnThreadInterrupted) {
        this.shutdownOnThreadInterrupted = shutdownOnThreadInterrupted;
    }

    private void closeAndDeleteAssociatedFileOnFailure(final FileInputStream fis, final File associatedFile) {
        try {
            fis.close();
        } catch (IOException e) {
            LOG.error("Couldn't close FileInputStream on {}, deleting the file!", associatedFile.getAbsolutePath(), e);
            if (associatedFile.exists() && !associatedFile.delete()) {
                LOG.error("Couldn't delete file {}", associatedFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Calling this method will result in writing all keys to be written to disk
     * or wait for the one in progress to finish
     *
     * @param localKeys the latest current local set
     * @throws IOException On exception being thrown while doing the snapshot
     */
    void snapshotNowOrWaitForCurrentToFinish(final Set localKeys) throws IOException {
        if (writeLock.tryLock()) {
            try {
                writeAll(localKeys);
            } finally {
                writeLock.unlock();
            }
        } else {
            writeLock.lock();
            writeLock.unlock();
        }
    }
}
