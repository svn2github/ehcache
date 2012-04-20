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
public class DiskStorePathManager {
    /**
     * If the CacheManager needs to resolve a conflict with the disk path, it will create a
     * subdirectory in the given disk path with this prefix followed by a number. The presence of this
     * name is used to determined whether it makes sense for a persistent DiskStore to be loaded. Loading
     * persistent DiskStores will only have useful semantics where the diskStore path has not changed.
     */
    public static final String AUTO_DISK_PATH_DIRECTORY_PREFIX = "ehcache_auto_created";
    private static final Logger LOG = LoggerFactory.getLogger(DiskStorePathManager.class);
    private static final String LOCK_FILE_NAME = ".ehcache-diskstore.lock";

    private static final int CHARSIZE = 4;
    private static final int DEL = 0x7F;
    private static final char ESCAPE = '%';
    private static final Set<Character> ILLEGALS = new HashSet<Character>();

    private final String diskStorePath;
    private FileLock thisDirectoryLock;
    private File lockFile;
    private FileChannel lockFileChannel;

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
     * Called by CacheManager
     *
     * @param diskStorePath
     */
    public DiskStorePathManager(String diskStorePath) {
        this.diskStorePath = validateAndLock(diskStorePath);
    }

    private String validateAndLock(String rawDiskStorePath) {
        // ensure disk store path exists
        File pathDir = new File(rawDiskStorePath);
        if (!pathDir.isDirectory() && !pathDir.mkdirs()) {
            throw new CacheException("Disk store path can't be created: " + rawDiskStorePath);
        }

        lockFile = new File(pathDir.getAbsoluteFile(), LOCK_FILE_NAME);
        lockFile.deleteOnExit();
        try {
            lockFile.createNewFile();
            if (!lockFile.exists()) {
                throw new AssertionError("Failed to create lock file " + lockFile);
            }
            lockFileChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            thisDirectoryLock = lockFileChannel.tryLock();

            // other process already holds the lock
            if (thisDirectoryLock == null) {
                String newDiskStorePath = rawDiskStorePath + File.separator + generateUniqueDirectory();
                warnAboutCollision(rawDiskStorePath, newDiskStorePath);
                return validateAndLock(newDiskStorePath);
            }

            return rawDiskStorePath;
        } catch (OverlappingFileLockException ofle) {
            // other thread has the lock
            String newDiskStorePath = rawDiskStorePath + File.separator + generateUniqueDirectory();
            warnAboutCollision(rawDiskStorePath, newDiskStorePath);
            return validateAndLock(newDiskStorePath);
        } catch (IOException ioe) {
            throw new CacheException(ioe);
        }
    }

    private void warnAboutCollision(String oldDiskStorePath, String newDiskStorePath) {
        LOG.warn("diskStorePath '" + oldDiskStorePath + "' is already used by an existing CacheManager.\n"
                + "The diskStore path for this CacheManager will be set to " + newDiskStorePath + ".\nTo avoid this"
                + " warning consider using the CacheManager factory methods to create a singleton CacheManager "
                + "or specifying a separate ehcache configuration (ehcache.xml) for each CacheManager instance.");

    }

    private static String sanitize(String name) {
        // reserve old way of doing things for backward compatibility
        String noForwardSlash = name.replace('/', '_');

        int len = noForwardSlash.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = noForwardSlash.charAt(i);
            if (c <= ' ' || c >= DEL || ILLEGALS.contains(c) || c == ESCAPE) {
                sb.append(ESCAPE);

                String toHex = Integer.toHexString(c);
                if (toHex.length() > CHARSIZE) {
                    throw new AssertionError(toHex);
                }

                // zero pad
                for (int pad = 0, n = CHARSIZE - toHex.length(); pad < n; pad++) {
                    sb.append('0');
                }

                sb.append(toHex);
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static String safeName(String name, String suffix) {
        return sanitize(name) + suffix;
    }

    private static void deleteFile(File f) {
        if (!f.delete()) {
            LOG.debug("Failed to delete file {}", f.getName());
        }
    }

    private static String generateUniqueDirectory() {
        return AUTO_DISK_PATH_DIRECTORY_PREFIX + "_" + System.currentTimeMillis();
    }

    /**
     * release the lock file used for collision detection
     * should be called when cache manager shutdowns
     */
    public void releaseLock() {
        if (thisDirectoryLock != null && thisDirectoryLock.isValid()) {
            try {
                thisDirectoryLock.release();
                lockFileChannel.close();
                deleteFile(lockFile);
            } catch (IOException e) {
                throw new CacheException("Failed to release disk store path lock file:" + lockFile, e);
            } finally {
                thisDirectoryLock = null;
            }
        }
    }

    /**
     *
     * @return diskstore path
     */
    public String getDiskStorePath() {
        return diskStorePath;
    }

    /**
     * Returns the data file for the cache
     * @param cache
     * @return data file
     */
    public File getDataFile(Ehcache cache) {
        return new File(diskStorePath, safeName(cache.getName(), ".data"));
    }

    /**
     * Returns the index file for the cache
     * @param cache
     * @return index file
     */
    public File getIndexFile(Ehcache cache) {
        return new File(diskStorePath, safeName(cache.getName(), ".index"));
    }
}
