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

package net.sf.ehcache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to hold the Terracotta configuration - either a pointer to the real config or a
 * container for embedded config.
 *
 * @author Alex Miller
 * @author Geert Bevin
 */
public class TerracottaConfiguration implements Cloneable {

    /**
     * Default clustered mode
     */
    public static final boolean DEFAULT_CLUSTERED = true;
    /**
     * Default value mode
     */
    public static final ValueMode DEFAULT_VALUE_MODE = ValueMode.SERIALIZATION;
    /**
     * Default coherent read behavior
     */
    public static final boolean DEFAULT_COHERENT_READS = true;
    /**
     * Default xa enabled
     */
    public static final boolean DEFAULT_CACHE_XA = false;
    /**
     * Default orphan eviction status
     */
    public static final boolean DEFAULT_ORPHAN_EVICTION = true;
    /**
     * Default orphan eviction period
     */
    public static final int DEFAULT_ORPHAN_EVICTION_PERIOD = 4;
    /**
     * Default local key cache status
     */
    public static final boolean DEFAULT_LOCAL_KEY_CACHE = false;
    /**
     * Default local key cache size
     */
    public static final int DEFAULT_LOCAL_KEY_CACHE_SIZE = 300000;
    /**
     * Default copy on read setting
     */
    public static final boolean DEFAULT_COPY_ON_READ = false;
    
    /**
     * Default cache coherence setting
     */
    public static final boolean DEFAULT_CACHE_COHERENT = true;
    
    /**
     * Default setting for synchronous-write
     */
    public static final boolean DEFAULT_SYNCHRONOUS_WRITES = false;
    
    /**
     * Default setting for storageStrategy
     */
    public static final StorageStrategy DEFAULT_STORAGE_STRATEGY = StorageStrategy.CLASSIC;

    /**
     * Represents whether values are stored with serialization in the clustered store
     * or through Terracotta clustered identity.
     *
     * @author amiller
     */
    public static enum ValueMode {
        /**
         * When a key or value is put in the cache, serialize the data for sending around the cluster
         */
        SERIALIZATION,

        /**
         * Use Terracotta clustered identity to preserve object identity without serialization
         */
        IDENTITY,
    }
    
    /**
     * Represents whether keys/values are to be stored in the local vm or the Terracotta server
     *
     * @author Abhishek Sanoujam
     */
    public static enum StorageStrategy {
        /**
         * Store the key/values in the local vm
         */
        CLASSIC,

        /**
         * Store the key/values in the Terracotta Server
         */
        DCV2,
    }

    private static final Logger LOG = LoggerFactory.getLogger(TerracottaConfiguration.class.getName());
    
    private boolean clustered = DEFAULT_CLUSTERED;
    private ValueMode valueMode = DEFAULT_VALUE_MODE;
    private boolean coherentReads = DEFAULT_COHERENT_READS;
    private boolean orphanEviction = DEFAULT_ORPHAN_EVICTION;
    private int orphanEvictionPeriod = DEFAULT_ORPHAN_EVICTION_PERIOD;
    private boolean localKeyCache = DEFAULT_LOCAL_KEY_CACHE;
    private int localKeyCacheSize = DEFAULT_LOCAL_KEY_CACHE_SIZE;
    private boolean isCopyOnRead = DEFAULT_COPY_ON_READ;
    private boolean cacheCoherent = DEFAULT_CACHE_COHERENT;
    private boolean cacheXA = DEFAULT_CACHE_XA;
    private boolean synchronousWrites = DEFAULT_SYNCHRONOUS_WRITES;
    private StorageStrategy storageStrategy = DEFAULT_STORAGE_STRATEGY;

    private boolean copyOnReadSet;

    /**
     * Clones this object, following the usual contract.
     *
     * @return a copy, which independent other than configurations than cannot change.
     */
    @Override
    public TerracottaConfiguration clone() {
        try {
            return (TerracottaConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Indicates whether to cluster this cache with Terracotta.
     * <p/>
     * Defaults to {@value #DEFAULT_CLUSTERED}.
     *
     * @param clustered {@code true} if the cache should be clustered with Terracotta; {@code false} otherwise
     */
    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    /**
     * @return this configuration instance
     * @see #setClustered(boolean)
     */
    public TerracottaConfiguration clustered(boolean clustered) {
        setClustered(clustered);
        return this;
    }

    /**
     * Check whether clustering is enabled
     */
    public boolean isClustered() {
        return this.clustered;
    }

    /**
     * Used by BeanHandler to set the copyOnRead flag during parsing
     */
    public void setCopyOnRead(boolean isCopyOnRead) {
        LOG.warn("copyOnRead is deprecated on the <terracotta /> element, " +
                 "please use the copyOnRead attribute on <cache /> or <defaultCache />");
        this.copyOnReadSet = true;
        this.isCopyOnRead = isCopyOnRead;
    }

    /**
     * Whether the copyOnRead was explicitly set 
     * @return true if set by config
     */
    boolean isCopyOnReadSet() {
        return copyOnReadSet;
    }

    /**
     * @return this configuration instance
     * @see #setCopyOnRead(boolean)
     */
    public TerracottaConfiguration copyOnRead(boolean isCopyOnRead) {
        setCopyOnRead(isCopyOnRead);
        return this;
    }

    /**
     * Check whether the [serialized value] cache is an XA enabled cache
     */
    public boolean isCacheXA() {
        return this.cacheXA;
    }
    
    /**
     * Used by BeanHandler to set the cacheXA flag during parsing
     */
    public void setCacheXA(boolean cacheXA) {
        this.cacheXA = cacheXA;
    }

    /**
     * @return this configuration instance
     * @see #setCacheXA(boolean)
     */
    public TerracottaConfiguration cacheXA(boolean cacheXA) {
        setCacheXA(cacheXA);
        return this;
    }

    /**
     * Check whether the [serialized value] cache should use copy on read semantics
     */
    public boolean isCopyOnRead() {
        return this.isCopyOnRead;
    }

    /**
     * Sets whether this cache should use coherent reads (usually should be {@value #DEFAULT_COHERENT_READS} unless optimizing for read-only).
     * <p/>
     * Defaults to {@value #DEFAULT_COHERENT_READS}.
     *
     * @param coherentReads {@code true} if coherent reads should be used; {@code false} otherwise
     */
    public void setCoherentReads(boolean coherentReads) {
        LOG.warn("The attribute \"coherentReads\" in \"terracotta\" element is deprecated."
                + " Please use the new \"coherent\" attribute instead.");
        this.coherentReads = coherentReads;
    }

    /**
     * @return this configuration instance
     * @see #setCoherentReads(boolean)
     */
    public TerracottaConfiguration coherentReads(boolean coherentReads) {
        setCoherentReads(coherentReads);
        return this;
    }

    /**
     * Check whether coherent reads are enabled
     */
    public boolean getCoherentReads() {
        return this.coherentReads;
    }

    /**
     * Converts the {@code valueMode} string argument to uppercase and looks up enum constant in ValueMode.
     */
    public void setValueMode(String valueMode) {
        if (valueMode == null) {
            throw new IllegalArgumentException("Value mode must be non-null");
        }
        this.valueMode = ValueMode.valueOf(ValueMode.class, valueMode.toUpperCase());
    }

    /**
     * @return this configuration instance
     * @see #setValueMode(String)
     */
    public TerracottaConfiguration valueMode(String valueMode) {
        setValueMode(valueMode);
        return this;
    }

    /**
     * @return this configuration instance
     * @see #setValueMode(String)
     */
    public TerracottaConfiguration valueMode(ValueMode valueMode) {
        if (valueMode == null) {
            throw new IllegalArgumentException("Value mode must be non-null");
        }
        this.valueMode = valueMode;
        return this;
    }

    /**
     * Get the value mode in terms of the mode enum
     */
    public ValueMode getValueMode() {
        return this.valueMode;
    }

    /**
     * Sets whether this cache should perform orphan eviction (usually should be {@value #DEFAULT_ORPHAN_EVICTION}).
     * <p/>
     * Orphans are elements that are not present on any node in the cluster anymore and hence need additional routines to be detected since they're not locally available anywhere.
     * <p/>
     * Defaults to {@value #DEFAULT_ORPHAN_EVICTION}.
     *
     * @param orphanEviction {@code true} if orphan eviction should be used; {@code false} otherwise
     */
    public void setOrphanEviction(boolean orphanEviction) {
        this.orphanEviction = orphanEviction;
    }

    /**
     * @return this configuration instance
     * @see #setOrphanEviction(boolean)
     */
    public TerracottaConfiguration orphanEviction(boolean orphanEviction) {
        setOrphanEviction(orphanEviction);
        return this;
    }

    /**
     * Check whether orphan eviction is enabled
     */
    public boolean getOrphanEviction() {
        return this.orphanEviction;
    }

    /**
     * Set how often this cache should perform orphan eviction (measured in regular eviction periods).
     * <p/>
     * Defaults to {@value #DEFAULT_ORPHAN_EVICTION_PERIOD}).
     *
     * @param orphanEvictionPeriod every how many regular evictions an orphan eviction should occur
     */
    public void setOrphanEvictionPeriod(int orphanEvictionPeriod) {
        this.orphanEvictionPeriod = orphanEvictionPeriod;
    }

    /**
     * @return this configuration instance
     * @see #setOrphanEvictionPeriod(int)
     */
    public TerracottaConfiguration orphanEvictionPeriod(int orphanEvictionPeriod) {
        setOrphanEvictionPeriod(orphanEvictionPeriod);
        return this;
    }

    /**
     * Get the number of regular eviction cycles between orphan evictions
     */
    public int getOrphanEvictionPeriod() {
        return this.orphanEvictionPeriod;
    }

    /**
     * Sets whether this cache should use an unclustered local key cache (usually should be {@value #DEFAULT_LOCAL_KEY_CACHE} unless optimizing for a small read-only cache)
     * <p/>
     * Defaults to {@value #DEFAULT_LOCAL_KEY_CACHE}.
     *
     * @param localKeyCache {@code true} if a local key cache should be used; {@code false} otherwise
     */
    public void setLocalKeyCache(boolean localKeyCache) {
        this.localKeyCache = localKeyCache;
    }

    /**
     * @return this configuration instance
     * @see #setLocalKeyCache(boolean)
     */
    public TerracottaConfiguration localKeyCache(boolean localKeyCache) {
        setLocalKeyCache(localKeyCache);
        return this;
    }

    /**
     * Check whether the local key cache is enabled
     */
    public boolean getLocalKeyCache() {
        return this.localKeyCache;
    }

    /**
     * Sets maximum size of the local key cache (usually the size of the key set of the cache or cache partition).
     * <p/>
     * Defaults to {@value #DEFAULT_LOCAL_KEY_CACHE_SIZE}.
     *
     * @param localKeyCacheSize the size of the local key cache in number of keys
     */
    public void setLocalKeyCacheSize(int localKeyCacheSize) {
        this.localKeyCacheSize = localKeyCacheSize;
    }

    /**
     * @return this configuration instance
     * @see #setLocalKeyCacheSize(int)
     */
    public TerracottaConfiguration localKeyCacheSize(int localKeyCacheSize) {
        setLocalKeyCacheSize(localKeyCacheSize);
        return this;
    }

    /**
     * Get the size limit of the local key cache (if enabled)
     */
    public int getLocalKeyCacheSize() {
        return this.localKeyCacheSize;
    }
    
    /**
     * Used by BeanHandler to set the <tt>coherent</tt> during parsing
     */
    public void setCoherent(boolean coherent) {
        this.cacheCoherent = coherent;
    }

    /**
     * @return this configuration instance
     * @see #setCoherent(boolean)
     */
    public TerracottaConfiguration coherent(boolean coherent) {
        setCoherent(coherent);
        return this;
    }

    /**
     * Is the cache configured for coherent or incoherent mode.
     * @return true if configured in coherent mode.
     */
    public boolean isCoherent() {
        return this.cacheCoherent;
    }

    /**
     * Is the cache configured for synchronous-write?
     * @return true if configured for synchronouse-write, otherwise false. Default is false
     */
    public boolean isSynchronousWrites() {
        return synchronousWrites;
    }

    /**
     * Set the value for synchronous-write
     * @param synchronousWrites true for using synchronous-write
     */
    public void setSynchronousWrites(boolean synchronousWrites) {
        this.synchronousWrites = synchronousWrites;
    }

    /**
     * @return this configuration instance
     * @see #setSynchronousWrites(boolean)
     */
    public TerracottaConfiguration synchronousWrites(boolean synchronousWrites) {
        setSynchronousWrites(synchronousWrites);
        return this;
    }
    
    /**
     * Converts the {@code storageStrategy} string argument to uppercase and looks up enum constant in StorageStrategy.
     */
    public void setStorageStrategy(String storageStrategy) {
        if (storageStrategy == null) {
            throw new IllegalArgumentException("Storage Strategy must be non-null");
        }
        this.storageStrategy = StorageStrategy.valueOf(StorageStrategy.class, storageStrategy.toUpperCase());
    }

    /**
     * @return this configuration instance
     * @see #setStorageStrategy(String)
     */
    public TerracottaConfiguration storageStrategy(String storageStrategy) {
        setStorageStrategy(storageStrategy);
        return this;
    }

    /**
     * @return this configuration instance
     * @see #setStorageStrategy(String)
     */
    public TerracottaConfiguration storageStrategy(StorageStrategy storageStrategy) {
        if (storageStrategy == null) {
            throw new IllegalArgumentException("Storage Strategy must be non-null");
        }
        this.storageStrategy = storageStrategy;
        return this;
    }

    /**
     * Get the value mode in terms of the mode enum
     */
    public StorageStrategy getStorageStrategy() {
        return this.storageStrategy;
    }
}
