/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.config;

import net.sf.ehcache.Cache;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;
import net.sf.ehcache.writer.writethrough.WriteThroughManager;

/**
 * Class to hold the CacheWriterManager configuration
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class CacheWriterConfiguration implements Cloneable {
    /**
     * Default writeMode
     */
    public static final WriteMode DEFAULT_WRITE_MODE = WriteMode.WRITE_THROUGH;
    /**
     * Default notifyListenersOnException behavior
     */
    public static final boolean DEFAULT_NOTIFY_LISTENERS_ON_EXCEPTION = false;
    /**
     * Default minimum write delay
     */
    public static final int DEFAULT_MIN_WRITE_DELAY = 1;
    /**
     * Default maximum write delay
     */
    public static final int DEFAULT_MAX_WRITE_DELAY = 1;
    /**
     * Default rate limit per second
     */
    public static final int DEFAULT_RATE_LIMIT_PER_SECOND = 0;
    /**
     * Default write coalescing behavior
     */
    public static final boolean DEFAULT_WRITE_COALESCING = false;
    /**
     * Default writeBatching behavior
     */
    public static final boolean DEFAULT_WRITE_BATCHING = false;
    /**
     * Default write batch size
     */
    public static final int DEFAULT_WRITE_BATCH_SIZE = 1;
    /**
     * Default retry attempts
     */
    public static final int DEFAULT_RETRY_ATTEMPTS = 0;
    /**
     * Default retry attempt delay
     */
    public static final int DEFAULT_RETRY_ATTEMPT_DELAY_SECONDS = 1;

    /**
     * Represents how elements are written to the {@link net.sf.ehcache.writer.CacheWriter}
     */
    public static enum WriteMode {
        /**
         * Write mode enum constant that can be used to configure a cache writer to use write through
         */
        WRITE_THROUGH {
            /**
             * {@inheritDoc}
             */
            @Override
            public CacheWriterManager createWriterManager(Cache cache) {
                return new WriteThroughManager();
            }
        },

        /**
         * Write mode enum constant that can be used to configure a cache writer to use write behind
         */
        WRITE_BEHIND {
            /**
             * {@inheritDoc}
             */
            @Override
            public CacheWriterManager createWriterManager(Cache cache) {
                return new WriteBehindManager();
            }
        };

        /**
         * Create a new {@code WriterManager} for a particular cache instance
         *
         * @param cache the cache instance for which the {@code WriterManager} should be created
         * @return the newly created {@code WriterManager}
         */
        public abstract CacheWriterManager createWriterManager(Cache cache);
    }

    private WriteMode writeMode = DEFAULT_WRITE_MODE;
    private boolean notifyListenersOnException = DEFAULT_NOTIFY_LISTENERS_ON_EXCEPTION;
    private int minWriteDelay = DEFAULT_MIN_WRITE_DELAY;
    private int maxWriteDelay = DEFAULT_MAX_WRITE_DELAY;
    private int rateLimitPerSecond = DEFAULT_RATE_LIMIT_PER_SECOND;
    private boolean writeCoalescing = DEFAULT_WRITE_COALESCING;
    private boolean writeBatching = DEFAULT_WRITE_BATCHING;
    private int writeBatchSize = DEFAULT_WRITE_BATCH_SIZE;
    private int retryAttempts = DEFAULT_RETRY_ATTEMPTS;
    private int retryAttemptDelaySeconds = DEFAULT_RETRY_ATTEMPT_DELAY_SECONDS;
    private CacheWriterFactoryConfiguration cacheWriterFactoryConfiguration;

    /**
     * Clones this object, following the usual contract.
     *
     * @return a copy, which independent other than configurations than cannot change.
     */
    @Override
    public CacheWriterConfiguration clone() {
        CacheWriterConfiguration config;
        try {
            config = (CacheWriterConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        if (cacheWriterFactoryConfiguration != null) {
            config.cacheWriterFactoryConfiguration = cacheWriterFactoryConfiguration.clone();
        }

        return config;
    }

    /**
     * Converts the {@code valueMode} string argument to uppercase and looks up enum constant in WriteMode.
     */
    public void setWriteMode(String writeMode) {
        if (writeMode == null) {
            throw new IllegalArgumentException("WriteMode can't be null");
        }
        this.writeMode = WriteMode.valueOf(WriteMode.class, writeMode.toUpperCase());
    }

    /**
     * @return this configuration instance
     * @see #setWriteMode(String)
     */
    public CacheWriterConfiguration writeMode(String writeMode) {
        setWriteMode(writeMode);
        return this;
    }

    /**
     * @return this configuration instance
     * @see #setWriteMode(String)
     */
    public CacheWriterConfiguration writeMode(WriteMode writeMode) {
        if (null == writeMode) {
            throw new IllegalArgumentException("WriteMode can't be null");
        }
        this.writeMode = writeMode;
        return this;
    }

    /**
     * Get the write mode in terms of the mode enum
     */
    public WriteMode getWriteMode() {
        return this.writeMode;
    }

    /**
     * Sets whether to notify listeners when an exception occurs on a writer operation.
     * <p/>
     * This is only applicable to write through mode.
     * <p/>
     * Defaults to {@value #DEFAULT_NOTIFY_LISTENERS_ON_EXCEPTION}.
     *
     * @param notifyListenersOnException {@code true} if listeners should be notified when an exception occurs on a writer operation; {@code false} otherwise
     */
    public void setNotifyListenersOnException(boolean notifyListenersOnException) {
        this.notifyListenersOnException = notifyListenersOnException;
    }

    /**
     * @return this configuration instance
     * @see #setNotifyListenersOnException(boolean)
     */
    public CacheWriterConfiguration notifyListenersOnException(boolean notifyListenersOnException) {
        setNotifyListenersOnException(notifyListenersOnException);
        return this;
    }

    /**
     * Check whether listeners should be notified when an exception occurs on a writer operation
     */
    public boolean getNotifyListenersOnException() {
        return this.notifyListenersOnException;
    }

    /**
     * Set the minimum number of seconds to wait before writing behind. If set to a value greater than 0, it permits
     * operations to build up in the queue. This is different from the maximum write delay in that by waiting a minimum
     * amount of time, work is always being built up. If the minimum write delay is set to zero and the {@code CacheWriter}
     * performs its work very quickly, the overhead of processing the write behind queue items becomes very noticeable
     * in a cluster since all the operations might be done for individual items instead of for a collection of them.  
     * <p/>
     * This is only applicable to write behind mode.
     * <p/>
     * Defaults to {@value #DEFAULT_MIN_WRITE_DELAY}).
     *
     * @param minWriteDelay the minimum number of seconds to wait before writing behind
     */
    public void setMinWriteDelay(int minWriteDelay) {
        this.minWriteDelay = minWriteDelay;
    }

    /**
     * @return this configuration instance
     * @see #setMinWriteDelay(int)
     */
    public CacheWriterConfiguration minWriteDelay(int minWriteDelay) {
        setMinWriteDelay(minWriteDelay);
        return this;
    }

    /**
     * Get the minimum number of seconds to wait before writing behind
     */
    public int getMinWriteDelay() {
        return this.minWriteDelay;
    }

    /**
     * Set the maximum number of seconds to wait before writing behind. If set to a value greater than 0, it permits
     * operations to build up in the queue to enable effective coalescing and batching optimisations.
     * <p/>
     * This is only applicable to write behind mode.
     * <p/>
     * Defaults to {@value #DEFAULT_MAX_WRITE_DELAY}).
     *
     * @param maxWriteDelay the maximum number of seconds to wait before writing behind
     */
    public void setMaxWriteDelay(int maxWriteDelay) {
        this.maxWriteDelay = maxWriteDelay;
    }

    /**
     * @return this configuration instance
     * @see #setMaxWriteDelay(int)
     */
    public CacheWriterConfiguration maxWriteDelay(int maxWriteDelay) {
        setMaxWriteDelay(maxWriteDelay);
        return this;
    }

    /**
     * Get the maximum number of seconds to wait before writing behind
     */
    public int getMaxWriteDelay() {
        return this.maxWriteDelay;
    }

    /**
     * Sets the maximum number of write operations to allow per second when {@link #writeBatching} is enabled.
     * <p/>
     * This is only applicable to write behind mode.
     * <p/>
     * Defaults to {@value #DEFAULT_RATE_LIMIT_PER_SECOND}.
     *
     * @param rateLimitPerSecond the number of write operations to allow; use a number {@code &lt;=0} to disable rate limiting.
     */
    public void setRateLimitPerSecond(int rateLimitPerSecond) {
        this.rateLimitPerSecond = rateLimitPerSecond;
    }

    /**
     * @return this configuration instance
     * @see #setRateLimitPerSecond(int rateLimitPerSecond)
     */
    public CacheWriterConfiguration rateLimitPerSecond(int rateLimitPerSecond) {
        setRateLimitPerSecond(rateLimitPerSecond);
        return this;
    }

    /**
     * Get the maximum number of write operations to allow per second.
     */
    public int getRateLimitPerSecond() {
        return rateLimitPerSecond;
    }

    /**
     * Sets whether to use write coalescing. If set to {@code true} and multiple operations on the same key are present
     * in the write-behind queue, only the latest write is done, as the others are redundant. This can dramatically
     * reduce load on the underlying resource.
     * <p/>
     * This is only applicable to write behind mode.
     * <p/>
     * Defaults to {@value #DEFAULT_WRITE_COALESCING}.
     *
     * @param writeCoalescing {@code true} to enable write coalescing; or {@code false} to disable it
     */
    public void setWriteCoalescing(boolean writeCoalescing) {
        this.writeCoalescing = writeCoalescing;
    }

    /**
     * @return this configuration instance
     * @see #setWriteCoalescing(boolean)
     */
    public CacheWriterConfiguration writeCoalescing(boolean writeCoalescing) {
        setWriteCoalescing(writeCoalescing);
        return this;
    }

    /**
     * @return this configuration instance
     * @see #setWriteCoalescing(boolean)
     */
    public boolean getWriteCoalescing() {
        return writeCoalescing;
    }

    /**
     * Sets whether to batch write operations. If set to {@code true}, {@link net.sf.ehcache.writer.CacheWriter#writeAll} and {@code CacheWriter#deleteAll}
     * will be called rather than {@link net.sf.ehcache.writer.CacheWriter#write} and {@link net.sf.ehcache.writer.CacheWriter#delete} being called for each key. Resources such
     * as databases can perform more efficiently if updates are batched, thus reducing load.
     * <p/>
     * This is only applicable to write behind mode.
     * <p/>
     * Defaults to {@value #DEFAULT_WRITE_BATCHING}.
     *
     * @param writeBatching {@code true} if write operations should be batched; {@code false} otherwise
     */
    public void setWriteBatching(boolean writeBatching) {
        this.writeBatching = writeBatching;
    }

    /**
     * @return this configuration instance
     * @see #setWriteBatching(boolean)
     */
    public CacheWriterConfiguration writeBatching(boolean writeBatching) {
        setWriteBatching(writeBatching);
        return this;
    }

    /**
     * Check whether write operations should be batched
     */
    public boolean getWriteBatching() {
        return this.writeBatching;
    }

    /**
     * Sets the number of operations to include in each batch when {@link #writeBatching} is enabled. If there are less
     * entries in the write-behind queue than the batch size, the queue length size is used.
     * <p/>
     * This is only applicable to write behind mode.
     * <p/>
     * Defaults to {@value #DEFAULT_WRITE_BATCH_SIZE}.
     *
     * @param writeBatchSize the number of operations to include in each bathc; numbers smaller than {@code 1} will cause
     *                       the default batch size to be used
     */
    public void setWriteBatchSize(int writeBatchSize) {
        this.writeBatchSize = writeBatchSize;
    }

    /**
     * @return this configuration instance
     * @see #setWriteBatchSize(int)
     */
    public CacheWriterConfiguration writeBatchSize(int writeBatchSize) {
        setWriteBatchSize(writeBatchSize);
        return this;
    }

    /**
     * Retrieves the size of the batch operation.
     */
    public int getWriteBatchSize() {
        return writeBatchSize;
    }

    /**
     * Sets the number of times the operation is retried in the {@code CacheWriter}, this happens after the
     * original operation.
     * <p/>
     * This is only applicable to write behind mode.
     * <p/>
     * Defaults to {@value #DEFAULT_RETRY_ATTEMPTS}.
     *
     * @param retryAttempts the number of retries for a particular element
     */
    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    /**
     * @return this configuration instance
     * @see #setRetryAttempts(int)
     */
    public CacheWriterConfiguration retryAttempts(int retryAttempts) {
        setRetryAttempts(retryAttempts);
        return this;
    }

    /**
     * Retrieves the number of times the write of element is retried.
     */
    public int getRetryAttempts() {
        return retryAttempts;
    }

    /**
     * Sets the number of seconds to wait before retrying an failed operation.
     * <p/>
     * This is only applicable to write behind mode.
     * <p/>
     * Defaults to {@value #DEFAULT_RETRY_ATTEMPT_DELAY_SECONDS}.
     *
     * @param retryAttemptDelaySeconds the number of seconds to wait before retrying an operation
     */
    public void setRetryAttemptDelaySeconds(int retryAttemptDelaySeconds) {
        this.retryAttemptDelaySeconds = retryAttemptDelaySeconds;
    }

    /**
     * @return this configuration instance
     * @see #setRetryAttemptDelaySeconds(int)
     */
    public CacheWriterConfiguration retryAttemptDelaySeconds(int retryAttemptDelaySeconds) {
        setRetryAttemptDelaySeconds(retryAttemptDelaySeconds);
        return this;
    }

    /**
     * Retrieves the number of seconds to wait before retrying an failed operation.
     */
    public int getRetryAttemptDelaySeconds() {
        return retryAttemptDelaySeconds;
    }

    /**
     * Configuration for the CacheWriterFactoryConfiguration.
     */
    public static final class CacheWriterFactoryConfiguration extends FactoryConfiguration<CacheWriterFactoryConfiguration> {
    }

    /**
     * Allows BeanHandler to add the CacheWriterFactory to the configuration.
     */
    public final void addCacheWriterFactory(CacheWriterFactoryConfiguration cacheWriterFactoryConfiguration) {
        this.cacheWriterFactoryConfiguration = cacheWriterFactoryConfiguration;
    }

    /**
     * @return this configuration instance
     * @see #addCacheWriterFactory(CacheWriterFactoryConfiguration)
     */
    public CacheWriterConfiguration cacheWriterFactory(CacheWriterFactoryConfiguration cacheWriterFactory) {
        addCacheWriterFactory(cacheWriterFactory);
        return this;
    }

    /**
     * Accessor
     *
     * @return the configuration
     */
    public CacheWriterFactoryConfiguration getCacheWriterFactoryConfiguration() {
        return cacheWriterFactoryConfiguration;
    }
}