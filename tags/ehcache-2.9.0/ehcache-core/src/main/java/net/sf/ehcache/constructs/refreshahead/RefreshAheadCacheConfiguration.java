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
package net.sf.ehcache.constructs.refreshahead;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Fluent configuration class for {@link RefreshAheadCache} instances.
 *
 * @author cschanck
 *
 */
public class RefreshAheadCacheConfiguration implements Cloneable {

    /**
     * Properties key for the batch size attribute
     */
    public static final String BATCH_SIZE_KEY = "batchSize";

    /**
     * Properties key for the batch size attribute
     */
    public static final String NUMBER_OF_THREADS_KEY = "numberOfThreads";

    /**
     * Properties key for the batch size attribute
     */
    public static final String NAME_KEY = "name";

    /**
     * Properties key for the batch size attribute
     */
    public static final String TIME_TO_REFRESH_SECONDS_KEY = "timeToRefreshSeconds";

    /**
     * Properties key for the max backlog attribute
     */
    public static final String MAX_BACKLOG = "maximumBacklogItems";

    /**
     * Properties key for the batch size attribute
     */
    public static final String EVICT_ON_LOAD_MISS = "evictOnLoadMiss";

    private static final int DEFAULT_NUMBER_THREADS = 1;
    private static final int DEFAULT_BATCHSIZE = 100;
    private static final int DEFAULT_BACKLOG_MAX = -1;

    private long timeToRefreshSeconds = Long.MAX_VALUE;
    private long timeToRefreshMillis = 0L;
    private int maximumRefreshBacklogItems = DEFAULT_BACKLOG_MAX;
    private int batchSize = DEFAULT_BATCHSIZE;
    private boolean evictOnLoadMiss = false;
    private int numberOfThreads = DEFAULT_NUMBER_THREADS;
    private String name = null;

    private volatile boolean valid = false;

    /**
     * Create a default, valid configuration
     */
    public RefreshAheadCacheConfiguration() {
    }

    /**
     * Initialize this configuration from a {@link Properties} object. Will be
     * validated before returning.
     *
     * @param properties
     * @return this configuration
     */
    public RefreshAheadCacheConfiguration fromProperties(Properties properties) {
        valid = false;
        if (properties != null) {
            for (String property : properties.stringPropertyNames()) {
                String stringValue = properties.getProperty(property).trim();
                if (TIME_TO_REFRESH_SECONDS_KEY.equals(property)) {
                    setTimeToRefreshSeconds(Long.parseLong(stringValue));
                } else if (NAME_KEY.equals(property)) {
                    setName(stringValue);
                } else if (NUMBER_OF_THREADS_KEY.equals(property)) {
                    setNumberOfThreads(Integer.parseInt(stringValue));
                } else if (BATCH_SIZE_KEY.equals(property)) {
                    setBatchSize(Integer.parseInt(stringValue));
                } else if (EVICT_ON_LOAD_MISS.equals(property)) {
                    setEvictOnLoadMiss(Boolean.parseBoolean(stringValue));
                } else if (MAX_BACKLOG.equals(property)) {
                    setMaximumRefreshBacklogItems(Integer.parseInt(stringValue));
                } else {
                    throw new IllegalArgumentException("Unrecognized RefreshAhead cache config key: " + property);
                }
            }
        }
        return build();
    }

    /**
     * Express this configuration as a {@link Properties} object.
     *
     * @return properties version of this config
     */
    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty(NAME_KEY, getName());
        p.setProperty(NUMBER_OF_THREADS_KEY, Long.toString(getNumberOfThreads()));
        p.setProperty(TIME_TO_REFRESH_SECONDS_KEY, Long.toString(getTimeToRefreshSeconds()));
        p.setProperty(BATCH_SIZE_KEY, Long.toString(getBatchSize()));
        p.setProperty(EVICT_ON_LOAD_MISS, Boolean.toString(isEvictOnLoadMiss()));
        p.setProperty(MAX_BACKLOG, Long.toString(getMaximumRefreshBacklogItems()));
        return p;
    }

    /**
     * Validate this configuration.
     *
     * @return validated configuration
     * @throws IllegalStateException
     */
    public RefreshAheadCacheConfiguration build() {
        validate();
        return this;
    }

    private void validate() {
        if (timeToRefreshSeconds <= 0L) {
            throw new IllegalStateException("Must provide >=0 timeToRefreshSeconds for refresh ahead caching");
        }
        if (maximumRefreshBacklogItems <= 0) {
            throw new IllegalStateException("Must provide >=0 maximumBacklogItems for refresh ahead caching");
        }
        valid = true;
    }

    private void checkValid() {
        if (!valid) {
            throw new IllegalStateException("InlineRefreshCacheConfig not built yet");
        }
    }

    /**
     * return the time to refresh in milliseconds.
     *
     * @return
     */
    public long getTimeToRefreshMillis() {
        return timeToRefreshMillis;
    }

    /**
     * Fluently set the time to refresh seconds
     *
     * @param secs seconds
     * @return this config
     */
    public RefreshAheadCacheConfiguration timeToRefreshSeconds(long secs) {
        setTimeToRefreshSeconds(secs);
        return this;
    }

    /**
     * Get the time to refresh in seconds
     *
     * @return time to refresh in seconds
     */
    public long getTimeToRefreshSeconds() {
        checkValid();
        return timeToRefreshSeconds;
    }

    /**
     * Set the time to refresh in seconds
     *
     * @param timeToRefreshSeconds
     */
    public void setTimeToRefreshSeconds(long timeToRefreshSeconds) {
        valid = false;
        this.timeToRefreshSeconds = timeToRefreshSeconds;
        this.timeToRefreshMillis = TimeUnit.MILLISECONDS.convert(this.timeToRefreshSeconds, TimeUnit.SECONDS);
    }

    /**
     * Get the maximum number of backlog items allowed. This is the
     * max number of items that this local decorator will allow to
     * be awaiting refresh at one time. If more requests are made than
     * this, requests will begin to be thrown on the floor.
     *
     * @return max refresh backlog count
     */
    public int getMaximumRefreshBacklogItems() {
        checkValid();
        return maximumRefreshBacklogItems;
    }

    /**
     * Fluently set the maximum refresh backlog items.
     *
     * @param maximumRefreshBacklogItems
     * @return
     */
    public RefreshAheadCacheConfiguration maximumRefreshBacklogItems(int maximumRefreshBacklogItems) {
        setMaximumRefreshBacklogItems(maximumRefreshBacklogItems);
        return this;
    }

    /**
     * Set the maximum refresh backlog items. This is the max number of items which can be queued for
     * refresh processing; above this, keys that are candidates for refresh may be skipped. The correct
     * setting for this will be deployment specific. Too low and refresh opportunities will be skipped;
     * too high and refresh operations could come to dominate processing.
     *
     * @param maximumRefreshBacklogItems
     */
    public void setMaximumRefreshBacklogItems(int maximumRefreshBacklogItems) {
        valid = false;
        this.maximumRefreshBacklogItems = maximumRefreshBacklogItems;
    }

    /**
     * Get the number of threads used locally in this instance to process
     * refresh requests
     * @return number of threads
     */
    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    /**
     * Set the number of threads used locally in this instance to process
     * refresh requests
     * @param numberOfThreads number of threads
     */
    public void setNumberOfThreads(int numberOfThreads) {
        valid = false;
        this.numberOfThreads = numberOfThreads;
    }

    /**
     * Fluently set the number of threads used locally in this instance to process
     * refresh requests
     * @param numberOfThreads number of threads
     * @return this config object
     */
    public RefreshAheadCacheConfiguration numberOfThreads(int numberOfThreads) {
        setNumberOfThreads(numberOfThreads);
        return this;
    }

    /**
     * Get the batch size with which refresh requests will be processed.
     * @return batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Set the batch size for processing refresh requests.
     * @param batchSize maximum batch size
     */
    public void setBatchSize(int batchSize) {
        valid = false;
        this.batchSize = batchSize;
    }

    /**
     * Fluently set the batch size for processing refresh requests.
     * @param batchSize maximum batch size
     * @return this configuration object
     */
    public RefreshAheadCacheConfiguration batchSize(int batchSize) {
        setBatchSize(batchSize);
        return this;
    }

    /**
     * Get the name of this cache decorator
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this cache decorator
     * @param name
     * @return
     */
    public void setName(String name) {
        valid = false;
        this.name = name;
    }

    /**
     * Fluently set the name of this cache decorator
     * @param name
     * @return
     */
    public RefreshAheadCacheConfiguration name(String name) {
        setName(name);
        return this;
    }

    /**
     * Get whether no return for a key from all CacheLoaders will force
     * an eviction prematurely from the underlying cache.
     */
    public boolean isEvictOnLoadMiss() {
        return evictOnLoadMiss;
    }

    /**
     * Set whether no return for a key from all CacheLoaders should force
     * an eviction prematurely from the underlying cache.
     *
     * @param loadMissEvicts true to evict
     */
    public void setEvictOnLoadMiss(boolean loadMissEvicts) {
        valid = false;
        this.evictOnLoadMiss = loadMissEvicts;
    }

    /**
     * Fluently set whether no return for a key from all CacheLoaders should force
     * an eviction prematurely from the underlying cache.
     *
     * @param loadMissEvicts true to evict
     * @return this config
     */
    public RefreshAheadCacheConfiguration evictOnLoadMiss(boolean loadMissEvicts) {
        setEvictOnLoadMiss(loadMissEvicts);
        return this;
    }

    @Override
    public String toString() {
        return "RefreshAheadCacheConfiguration:  " + toProperties().toString();
    }

}
