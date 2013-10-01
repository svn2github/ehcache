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

package net.sf.ehcache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to hold the Terracotta configuration - either a pointer to the real config or a
 * container for embedded config.
 *
 * @author Alex Miller
 * @author Geert Bevin
 * @author Abhishek Sanoujam
 */
public class TerracottaConfiguration implements Cloneable {

    /**
     * Default clustered mode
     */
    public static final boolean DEFAULT_CLUSTERED = true;
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
     * Default compression setting
     */
    public static final boolean DEFAULT_COMPRESSION_ENABLED = false;

    /**
     * Default value for {@link NonstopConfiguration}
     */
    public static final NonstopConfiguration DEFAULT_NON_STOP_CONFIGURATION = makeDefaultNonstopConfiguration();


    /**
     * Default cache coherence setting
     *
     * @deprecated since 2.4 Use {@link #DEFAULT_CONSISTENCY_TYPE} instead.
     */
    @Deprecated
    public static final boolean DEFAULT_CACHE_COHERENT = true;

    /**
     * Default cache consistency setting
     */
    public static final Consistency DEFAULT_CONSISTENCY_TYPE = Consistency.EVENTUAL;

    /**
     * Default setting for synchronous-write
     */
    public static final boolean DEFAULT_SYNCHRONOUS_WRITES = false;

    /**
     * Default value for concurrency of the internal Store.
     */
    public static final int DEFAULT_CONCURRENCY = 0;


    /**
     * Default value for whether local cache is enabled or not
     */
    public static final boolean DEFAULT_LOCAL_CACHE_ENABLED = true;

    private static final Logger LOG = LoggerFactory.getLogger(TerracottaConfiguration.class.getName());

    private boolean clustered = DEFAULT_CLUSTERED;
    private boolean coherentReads = DEFAULT_COHERENT_READS;
    private boolean orphanEviction = DEFAULT_ORPHAN_EVICTION;
    private int orphanEvictionPeriod = DEFAULT_ORPHAN_EVICTION_PERIOD;
    private boolean localKeyCache = DEFAULT_LOCAL_KEY_CACHE;
    private int localKeyCacheSize = DEFAULT_LOCAL_KEY_CACHE_SIZE;
    private boolean isCopyOnRead = DEFAULT_COPY_ON_READ;
    private boolean cacheXA = DEFAULT_CACHE_XA;
    private boolean synchronousWrites = DEFAULT_SYNCHRONOUS_WRITES;
    private int concurrency = DEFAULT_CONCURRENCY;
    private NonstopConfiguration nonStopConfiguration = makeDefaultNonstopConfiguration();

    private boolean copyOnReadSet;
    private Consistency consistency = DEFAULT_CONSISTENCY_TYPE;
    private volatile boolean localCacheEnabled = DEFAULT_LOCAL_CACHE_ENABLED;
    private volatile boolean compressionEnabled = DEFAULT_COMPRESSION_ENABLED;

    /**
     * Clones this object, following the usual contract.
     *
     * @return a copy, which independent other than configurations than cannot change.
     */
    @Override
    public TerracottaConfiguration clone() {
        try {
            TerracottaConfiguration clone = (TerracottaConfiguration) super.clone();
            if (nonStopConfiguration != null) {
                clone.nonstop(this.nonStopConfiguration.clone());
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertArgumentNotNull(String name, Object object) {
        if (object == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    /**
     * Indicates whether to cluster this cache with Terracotta.
     * <p/>
     * Defaults to {@value #DEFAULT_CLUSTERED}.
     *
     * @param clustered
     *            {@code true} if the cache should be clustered with Terracotta; {@code false} otherwise
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
    @Deprecated
    public void setCopyOnRead(boolean isCopyOnRead) {
        LOG.warn("copyOnRead is deprecated on the <terracotta /> element, "
                + "please use the copyOnRead attribute on <cache /> or <defaultCache />");
        this.copyOnReadSet = true;
        this.isCopyOnRead = isCopyOnRead;
    }

    /**
     * Used by BeanHandler to set the compressionEnaled flag during parsing
     */
    public void setCompressionEnabled(boolean enabled) {
        this.compressionEnabled = enabled;
    }

    /**
     * Whether the copyOnRead was explicitly set
     *
     * @return true if set by config
     */
    boolean isCopyOnReadSet() {
        return copyOnReadSet;
    }

    /**
     * @return this configuration instance
     * @see #setCopyOnRead(boolean)
     */
    @Deprecated
    public TerracottaConfiguration copyOnRead(boolean isCopyOnRead) {
        setCopyOnRead(isCopyOnRead);
        return this;
    }

    /**
     * @return this configuration instance
     */
    public TerracottaConfiguration compressionEnabled(boolean enabled) {
        setCompressionEnabled(enabled);
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
    @Deprecated
    public boolean isCopyOnRead() {
        return this.isCopyOnRead;
    }

    /**
     * Check whether compression is enabled
     */
    public boolean isCompressionEnabled() {
        return this.compressionEnabled;
    }

    /**
     * Sets whether this cache should use coherent reads (usually should be {@value #DEFAULT_COHERENT_READS} unless optimizing for
     * read-only).
     * <p/>
     * Defaults to {@value #DEFAULT_COHERENT_READS}.
     *
     * @param coherentReads
     *            {@code true} if coherent reads should be used; {@code false} otherwise
     */
    @Deprecated
    public void setCoherentReads(boolean coherentReads) {
        LOG.warn("The attribute \"coherentReads\" in \"terracotta\" element is deprecated."
                + " Please use the new \"coherent\" attribute instead.");
        this.coherentReads = coherentReads;
    }

    /**
     * @return this configuration instance
     * @see #setCoherentReads(boolean)
     */
    @Deprecated
    public TerracottaConfiguration coherentReads(boolean coherentReads) {
        setCoherentReads(coherentReads);
        return this;
    }

    /**
     * Check whether coherent reads are enabled
     */
    @Deprecated
    public boolean getCoherentReads() {
        return this.coherentReads;
    }

    /**
     * Sets whether this cache should perform orphan eviction (usually should be {@value #DEFAULT_ORPHAN_EVICTION}).
     * <p/>
     * Orphans are elements that are not present on any node in the cluster anymore and hence need additional routines to be detected since
     * they're not locally available anywhere.
     * <p/>
     * Defaults to {@value #DEFAULT_ORPHAN_EVICTION}.
     *
     * @param orphanEviction
     *            {@code true} if orphan eviction should be used; {@code false} otherwise
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
     * @param orphanEvictionPeriod
     *            every how many regular evictions an orphan eviction should occur
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
     * Sets whether this cache should use an unclustered local key cache (usually should be {@value #DEFAULT_LOCAL_KEY_CACHE} unless
     * optimizing for a small read-only cache)
     * <p/>
     * Defaults to {@value #DEFAULT_LOCAL_KEY_CACHE}.
     *
     * @param localKeyCache
     *            {@code true} if a local key cache should be used; {@code false} otherwise
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
     * @param localKeyCacheSize
     *            the size of the local key cache in number of keys
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
     *
     * @deprecated since 2.4 Use {@link #setConsistency(Consistency)} instead
     */
    @Deprecated
    public void setCoherent(boolean coherent) {
        Consistency consistencyType = coherent ? Consistency.STRONG : Consistency.EVENTUAL;
        this.consistency(consistencyType);
    }

    /**
     * @return this configuration instance
     * @deprecated since 2.4 Use {@link #consistency(Consistency)} instead
     */
    @Deprecated
    public TerracottaConfiguration coherent(boolean coherent) {
        Consistency consistencyType = coherent ? Consistency.STRONG : Consistency.EVENTUAL;
        this.consistency(consistencyType);
        return this;
    }

    /**
     * Is the cache configured for coherent or incoherent mode.
     *
     * @return true if configured in coherent mode.
     * @deprecated since 2.4 Use {@link #getConsistency()} instead to query the {@link Consistency} or Ehcache#isNodeCoherent()
     *             to query if the node is coherent
     */
    @Deprecated
    public boolean isCoherent() {
        return consistency == Consistency.STRONG;
    }

    /**
     * Is the cache configured for synchronous-write?
     *
     * @return true if configured for synchronouse-write, otherwise false. Default is false
     */
    public boolean isSynchronousWrites() {
        return synchronousWrites;
    }

    /**
     * Set the value for synchronous-write
     *
     * @param synchronousWrites
     *            true for using synchronous-write
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
     * @return this configuration instance
     * @see #setConcurrency(int)
     */
    public TerracottaConfiguration concurrency(final int concurrency) {
        setConcurrency(concurrency);
        return this;
    }

    /**
     * Sets the value of concurrency. Throws {@link IllegalArgumentException} if the value is less than 0.
     * This value cannot be changed once cache is initialized.
     */
    public void setConcurrency(final int concurrency) {
        if (concurrency < 0) {
            throw new IllegalArgumentException("Only non-negative integers allowed");
        }
        this.concurrency = concurrency;
    }

    /**
     * Get the value of concurrency.
     * This value cannot be changed once cache is initialized.
     */
    public int getConcurrency() {
        return this.concurrency;
    }

    /**
     * Add the {@link NonstopConfiguration}
     *
     * @param nonstopConfiguration
     */
    public void addNonstop(NonstopConfiguration nonstopConfiguration) {
        this.nonStopConfiguration = nonstopConfiguration;
    }

    /**
     * Set the {@link NonstopConfiguration}
     *
     * @param nonstopConfiguration
     * @return this configuration instance
     */
    public TerracottaConfiguration nonstop(NonstopConfiguration nonstopConfiguration) {
        this.addNonstop(nonstopConfiguration);
        return this;
    }

    /**
     * Get the {@link NonstopConfiguration}, may be null
     *
     * @return the {@link NonstopConfiguration}, may be null
     */
    public NonstopConfiguration getNonstopConfiguration() {
        return nonStopConfiguration;
    }

    /**
     * Returns true if nonstop is enabled
     *
     * @return true if nonstop is enabled
     */
    public boolean isNonstopEnabled() {
        return nonStopConfiguration != null && nonStopConfiguration.isEnabled();
    }

    /**
     * Setter for consistency, returns this instance
     *
     * @param consistency
     * @return this instance
     */
    public TerracottaConfiguration consistency(Consistency consistency) {
        setConsistency(consistency);
        return this;
    }

    /**
     * Setter for consistency
     *
     * @param consistency
     */
    public void setConsistency(Consistency consistency) {
        this.consistency = consistency;
    }

    /**
     * Setter for consistency
     *
     * @param consistency
     */
    public void setConsistency(String consistency) {
        if (consistency == null) {
            throw new IllegalArgumentException("Consistency cannot be null");
        }
        this.setConsistency(Consistency.valueOf(consistency.toUpperCase()));
    }

    /**
     * Getter for consistency
     *
     * @return the consistency
     */
    public Consistency getConsistency() {
        return this.consistency;
    }

    /**
     * Returns true if local cache is enabled, otherwise false
     * @return true if local cache is enabled, otherwise false
     */
    public boolean isLocalCacheEnabled() {
        return localCacheEnabled;
    }

    /**
     * Enable or disable the local cache
     * @param localCacheEnabled
     */
    public void setLocalCacheEnabled(final boolean localCacheEnabled) {
        this.localCacheEnabled = localCacheEnabled;
    }


    /**
     * Enable or disable the local cache
     * @param localCacheEnabled
     * @return this instance
     */
    public TerracottaConfiguration localCacheEnabled(final boolean localCacheEnabled) {
        setLocalCacheEnabled(localCacheEnabled);
        return this;
    }

    /**
     * Enum for various consistency settings
     *
     * @author Abhishek Sanoujam
     *
     */
    public static enum Consistency {
        /**
         * Strong consistency
         */
        STRONG,
        /**
         * Eventual consistency
         */
        EVENTUAL;
    }

    /*
     * It is important that each instance of TerracottaConfiguration gets it's own, initial, default
     * NonstopConfiguration. Otherwise, getNonstopConfiguration().blah(...) calls can change the
     * default instance, leading to weirdness in writing out the xml and other places.
     */
    private static NonstopConfiguration makeDefaultNonstopConfiguration() {
        return new NonstopConfiguration().enabled(false);
    }
}
