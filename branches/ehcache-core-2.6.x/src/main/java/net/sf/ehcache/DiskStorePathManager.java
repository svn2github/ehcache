/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class to handle disk store path. CacheManager has a reference to this manager.
 *
 * @author hhuynh
 *
 */
public final class DiskStorePathManager {
    /**
     * If the CacheManager needs to resolve a conflict with the disk path, it will create a
     * subdirectory in the given disk path with this prefix followed by a number. The presence of this
     * name is used to determined whether it makes sense for a persistent DiskStore to be loaded. Loading
     * persistent DiskStores will only have useful semantics where the diskStore path has not changed.
     */
    public static final String AUTO_DISK_PATH_DIRECTORY_PREFIX = "ehcache_auto_created";
    private static final Logger LOG = LoggerFactory.getLogger(DiskStorePathManager.class);
    private static final String LOCK_FILE_NAME = ".ehcache-diskstore.lock";

    private static final int DEL = 0x7F;
    private static final char ESCAPE = '%';
    private static final Set<Character> ILLEGALS = new HashSet<Character>();

    private final File diskStorePath;
    private final FileLock thisDirectoryLock;
    private final File lockFile;

    static {
        ILLEGALS.add('/');
        ILLEGALS.add('\\');
        ILLEGALS.add('<');
        ILLEGALS.add('>');
        ILLEGALS.add(':');
        ILLEGALS.add('"');
        ILLEGALS.add('|');
        ILLEGALS.add('?');
        ILLEGALS.add('*');
        ILLEGALS.add('.');
    }

    /**
     * private constructor
     *
     * @param diskStorePath
     * @throws DiskstoreNotExclusiveException
     */
    private DiskStorePathManager(File path) throws DiskstoreNotExclusiveException {
        FileLock directoryLock;

        // ensure disk store path exists
        if (!path.isDirectory() && !path.mkdirs()) {
            throw new CacheException("Disk store path can't be created: " + path);
        }

        lockFile = new File(path.getAbsoluteFile(), LOCK_FILE_NAME);
        lockFile.deleteOnExit();
        try {
            lockFile.createNewFile();
            if (!lockFile.exists()) {
                throw new AssertionError("Failed to create lock file " + lockFile);
            }
            FileChannel lockFileChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            directoryLock = lockFileChannel.tryLock();
        } catch (OverlappingFileLockException ofle) {
            directoryLock = null;
        } catch (IOException ioe) {
            throw new CacheException(ioe);
        }

        if (directoryLock == null) {
            throw new DiskstoreNotExclusiveException(path.getAbsolutePath() + " is not exclusive.");
        }

        thisDirectoryLock = directoryLock;
        diskStorePath = path;
        LOG.debug("Using diskstore path {}", diskStorePath);
        LOG.debug("Holding exclusive lock on {}", lockFile);
    }

    /**
     * Create a diskstore path manager with provided path with exclusive access
     *
     * @param path
     * @return diskstore manager instance
     */
    public static final DiskStorePathManager createInstance(String path) {
        DiskStorePathManager manager = null;
        File candidate = new File(path);
        do {
            try {
                manager = new DiskStorePathManager(candidate);
            } catch (DiskstoreNotExclusiveException e) {
                try {
                    candidate = File.createTempFile(AUTO_DISK_PATH_DIRECTORY_PREFIX, "diskstore", new File(path));
                    // we want to create a directory with this temp name so deleting the file first
                    candidate.delete();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        } while (manager == null);
        if (candidate.getName().startsWith(AUTO_DISK_PATH_DIRECTORY_PREFIX)) {
            LOG.warn("diskStorePath '" + path
                    + "' is already used by an existing CacheManager either in the same VM or in a different process.\n"
                    + "The diskStore path for this CacheManager will be set to " + candidate + ".\nTo avoid this"
                    + " warning consider using the CacheManager factory methods to create a singleton CacheManager "
                    + "or specifying a separate ehcache configuration (ehcache.xml) for each CacheManager instance.");
        }
        return manager;
    }

    /**
     * sanitize a name for valid file or directory name
     *
     * @param name
     * @return sanitized version of name
     */
    private static String safeName(String name) {
        int len = name.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            if (c <= ' ' || c >= DEL || (c >= 'A' && c <= 'Z') || ILLEGALS.contains(c) || c == ESCAPE) {
                sb.append(ESCAPE);
                sb.append(String.format("%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void deleteFile(File f) {
        if (!f.delete()) {
            LOG.debug("Failed to delete file {}", f.getAbsolutePath());
        }
    }

    /**
     * release the lock file used for collision detection
     * should be called when cache manager shutdowns
     */
    public synchronized void releaseLock() {
        if (thisDirectoryLock != null && thisDirectoryLock.isValid()) {
            try {
                thisDirectoryLock.release();
                thisDirectoryLock.channel().close();
                deleteFile(lockFile);
            } catch (IOException e) {
                throw new CacheException("Failed to release disk store path's lock file:" + lockFile, e);
            }
        }
    }

    /**
     * Legacy way of creating an index file
     *
     * @param cacheName
     * @return
     */
    public File getIndexFile(String cacheName) {
        return new File(diskStorePath, safeName(cacheName) + ".index");
    }

    /**
     * Legacy way of creating a data file
     *
     * @param cacheName
     * @return
     */
    public File getDataFile(String cacheName) {
        return new File(diskStorePath, safeName(cacheName) + ".data");
    }

    /**
     * Create snapshots file. Used by RotatingSnapshotFile
     *
     * @param cacheName
     * @param suffix
     * @return
     */
    public File getSnapshotFile(String cacheName, String suffix) {
        return new File(diskStorePath, safeName(cacheName) + suffix);
    }

    /**
     * Exception class thrown when a diskstore path collides with an existing one
     *
     */
    private static class DiskstoreNotExclusiveException extends Exception {

        /**
         * Constructor for the DiskstoreNotExclusiveException object.
         */
        public DiskstoreNotExclusiveException() {
            super();
        }

        /**
         * Constructor for the DiskstoreNotExclusiveException object.
         *
         * @param message the exception detail message
         */
        public DiskstoreNotExclusiveException(String message) {
            super(message);
        }
    }
}
