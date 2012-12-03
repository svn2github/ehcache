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

package net.sf.ehcache.constructs.scheduledrefresh;

import net.sf.ehcache.Ehcache;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for a {@link ScheduledRefreshCacheExtension}. Can be
 * initialized from a {@link Properties} object. Currently, the use of a clustered
 * {@link org.terracotta.quartz.TerracottaJobStore} is not supported. This usage
 * will be supported in the future.
 *
 * @author cschanck
 */
public class ScheduledRefreshConfiguration implements Serializable, Cloneable {

    /**
     * Properties key for the batch size attribute
     */
    public static final String PROP_BATCH_SIZE_KEY = "batchSize";

    /**
     * Properties key for the key generator class name
     */
    public static final String PROP_KEY_GENERATOR_CLASS = "keyGenerator";

    /**
     * Properties key for cron expression used to schedule this job
     */
    public static final String PROP_CRON_SCHEDULE = "cronExpression";

    /**
     * Properties key for enabling bulk load mode prior to exection of the
     * refresh
     */
    public static final String PROP_USE_BULKLOAD = "useBulkload";

    /**
     * Properties key for the quartz job count attribute
     */
    public static final String PROP_LOCAL_QUARTZ_JOB_COUNT = "quartzJobCount";

    /**
     * Properties key for Terracotta configuration url
     */
    public static final String PROP_TC_CONFIG_URL = "tcConfigUrl";

    /**
     * Properties key for the unique name identifier
     */
    public static final String PROP_UNIQUE_NAME = "uniqueName";

    /**
     * Properties key for the seed job polling interval
     */
    public static final String PROP_POLL_TIME_MS = "pollTimeMs";

    /**
     * Properties key for evictions on refresh fail
     */
    public static final String PROP_LOAD_MISS_EVICTS = "loadMissEvicts";

    /**
     * Properties key for misfires being scheduled immediately
     */
    public static final String PROP_SCHEDULE_MISFIRES_NOW = "scheduleMisfiresNow";

    /**
     * Default setting for null eviction.
     */
    public static final boolean DEFAULT_NULL_EVICTS = true;

    /**
     * Default setting for using bulkload.
     */
    public static final boolean DEFAULT_USE_BULKLOAD = false;

    /**
     * Default batch size for key refresh processing.
     */
    public static final int DEFAULT_BATCHSIZE = 100;

    /**
     * Default simultaneous Quartz thread count.
     */
    public static final int DEFAULT_QUARTZ_THREADCOUNT = 2;

    /**
     * Default setting for whterh job misfires are scheduled as soon as possible.
     */
    public static final boolean DEFAULT_SCHEDULE_MISFIRES_NOW = false;

    /**
     * Default polling timeout for monitoring refresh jobs.
     */
    public static final int DEFAULT_POLL_TIME_MS = (int) TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS);

    private static final long serialVersionUID = -6877036694574988955L;

    private int batchSize = DEFAULT_BATCHSIZE;
    private boolean useBulkload = DEFAULT_USE_BULKLOAD;
    private String cronExpression = null;
    private int quartzThreadCount = DEFAULT_QUARTZ_THREADCOUNT;
    private String terracottaConfigUrl = null;
    private String keyGeneratorClass = SimpleScheduledRefreshKeyGenerator.class.getName();
    private String uniqueNamePart = null;
    private int pollTimeMs = DEFAULT_POLL_TIME_MS;
    private boolean loadMissEvicts = DEFAULT_NULL_EVICTS;
    private boolean scheduleMisfiresNow = DEFAULT_SCHEDULE_MISFIRES_NOW;
    private volatile boolean valid = false;

    /**
     * Create a default, valid configuration
     */
    public ScheduledRefreshConfiguration() {
    }

    /**
     * Initialize this configuration from a {@link Properties} object. Will be
     * validated before returning.
     *
     * @param properties
     * @return this configuration
     */
    public ScheduledRefreshConfiguration fromProperties(Properties properties) {
        valid = false;
        if (properties != null) {
            for (String property : properties.stringPropertyNames()) {
                String stringValue = properties.getProperty(property).trim();
                if (PROP_BATCH_SIZE_KEY.equals(property)) {
                    setBatchSize(Integer.parseInt(stringValue));
                } else if (PROP_USE_BULKLOAD.equals(property)) {
                    setUseBulkload(Boolean.parseBoolean(stringValue));
                } else if (PROP_CRON_SCHEDULE.equals(property)) {
                    setCronExpression(stringValue);
                } else if (PROP_LOCAL_QUARTZ_JOB_COUNT.equals(property)) {
                    setQuartzThreadCount(Integer.parseInt(stringValue));
                } else if (PROP_POLL_TIME_MS.equals(property)) {
                    setPollTimeMs(Integer.parseInt(stringValue));
                } else if (PROP_TC_CONFIG_URL.equals(property)) {
                    setTerracottaConfigUrl(stringValue);
                } else if (PROP_LOAD_MISS_EVICTS.equals(property)) {
                    setLoadMissEvicts(Boolean.parseBoolean(stringValue));
                } else if (PROP_SCHEDULE_MISFIRES_NOW.equals(property)) {
                    setScheduleMisfiresNow(Boolean.parseBoolean(stringValue));
                } else if (PROP_KEY_GENERATOR_CLASS.equals(property)) {
                    setKeyGeneratorClass(stringValue);
                } else {
                    throw new IllegalArgumentException("Unrecognized Schedule Refresh cache config key: " + property);
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
        p.setProperty(PROP_BATCH_SIZE_KEY, Long.toString(getBatchSize()));
        p.setProperty(PROP_USE_BULKLOAD, Boolean.toString(isUseBulkload()));
        p.setProperty(PROP_LOAD_MISS_EVICTS, Boolean.toString(isLoadMissEvicts()));
        p.setProperty(PROP_SCHEDULE_MISFIRES_NOW, Boolean.toString(isScheduleMisfiresNow()));
        p.setProperty(PROP_CRON_SCHEDULE, getCronExpression());
        p.setProperty(PROP_LOCAL_QUARTZ_JOB_COUNT, Integer.toString(getQuartzThreadCount()));
        p.setProperty(PROP_POLL_TIME_MS, Integer.toString(getPollTimeMs()));
        p.setProperty(PROP_KEY_GENERATOR_CLASS, getKeyGeneratorClass());
        if (getTerracottaConfigUrl() != null) {
            p.setProperty(PROP_TC_CONFIG_URL, getTerracottaConfigUrl());
        }
        return p;
    }

    /**
     * Validate and mark this configuration good to use.
     *
     * @return validated configuration
     * @throws IllegalStateException
     */
    public ScheduledRefreshConfiguration build() {
        validate();
        return this;
    }

    /**
     * Validate this configuration.
     */
    public void validate() {
        if (cronExpression == null) {
            throw new IllegalArgumentException("Cron Schedule cannot be unspecified");
        }
        valid = true;
    }

    /**
     * is this configuration valid to use?
     *
     * @return true if it is valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Get the batch size with which refresh requests will be processed.
     *
     * @return batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Set the batch size for processing refresh requests. This is the number of
     * keys will be processed in a batch.
     *
     * @param batchSize maximum batch size
     */
    public void setBatchSize(int batchSize) {
        valid = false;
        this.batchSize = batchSize;
    }

    /**
     * Fluently set the batch size for processing refresh requests.
     *
     * @param batchSize maximum batch size
     * @return this configuration object
     */
    public ScheduledRefreshConfiguration batchSize(int batchSize) {
        setBatchSize(batchSize);
        return this;
    }

    /**
     * Get whether the cache will be put in bulk load mode prior to refresh.
     *
     * @return true if bulk load mode will be used for loading
     */
    public boolean isUseBulkload() {
        return useBulkload;
    }

    /**
     * Set the flag to use bulk load for refreshing the keys. If true, the cache
     * will be put in bulkLoade mode prior to running the refresh, and after all
     * the jobs are finished, it will be restored to it's prior state.
     *
     * @param useBulkload
     */
    public void setUseBulkload(boolean useBulkload) {
        valid = false;
        this.useBulkload = useBulkload;
    }

    /**
     * Fluently set the bulk load flag.
     *
     * @param yes
     * @return this configuration
     */
    public ScheduledRefreshConfiguration useBulkload(boolean yes) {
        setUseBulkload(yes);
        return this;
    }

    /**
     * Return the string cron expression which will be passed to Quartz to
     * schedule the refresh.
     *
     * @return cron expression string
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Set the cron expression Quartz will use for scheduling this refresh job.
     * See Quartz documentation for a further explanation.
     *
     * @param cronExpression
     */
    public void setCronExpression(String cronExpression) {
        valid = false;
        this.cronExpression = cronExpression;
    }

    /**
     * Fluently set the cron expression Quartz will use for scheduling this
     * refresh job
     *
     * @param cronExpression
     * @return this configuration
     */
    public ScheduledRefreshConfiguration cronExpression(String cronExpression) {
        setCronExpression(cronExpression);
        return this;
    }

    /**
     * Get the quartz thread count.
     *
     * @return the quartz thread count
     */
    public int getQuartzThreadCount() {
        return quartzThreadCount;
    }

    /**
     * Set the Quartz thread count. This is the number of concurrent refresh
     * batches which can be processed at one time. The overseeing job will poll
     * and not schedule more than this many jobs at one time.
     *
     * @param quartzThreadCount
     */
    public void setQuartzThreadCount(int quartzThreadCount) {
        valid = false;
        this.quartzThreadCount = quartzThreadCount;
    }

    /**
     * Fluently set the Quartz thread count.
     *
     * @param quartzThreadCount
     * @return this configuration
     */
    public ScheduledRefreshConfiguration quartzThreadCount(int quartzThreadCount) {
        setQuartzThreadCount(quartzThreadCount);
        return this;
    }

    /**
     * Get the key generator class used to generate the list of keys to refresh.
     *
     * @return the fully qualified class name of the {@link ScheduledRefreshKeyGenerator} class
     */
    public String getKeyGeneratorClass() {
        return keyGeneratorClass;
    }

    /**
     * Set the key generator class used to generate the list of keys to refresh.
     * This is the class used to generate keys from the target cache. A simple
     * implementation of the naive getKeys() approach is supplied.
     *
     * @param keyGeneratorClass
     */
    public void setKeyGeneratorClass(String keyGeneratorClass) {
        this.keyGeneratorClass = keyGeneratorClass;
    }

    /**
     * Fluently set the key generator class used to generate the list of keys to
     * refresh.
     *
     * @param keyGeneratorClass
     * @return this configuration
     */
    public ScheduledRefreshConfiguration keyGeneratorClass(String keyGeneratorClass) {
        setKeyGeneratorClass(keyGeneratorClass);
        return this;
    }

    /**
     * Get the Terracotta configuration url, to use a TerracottaJobStore. If
     * this is not set, a RamJobStore will be used.
     *
     * @return the Terracotta cluster url used for the TerracottJobStore
     */
    public String getTerracottaConfigUrl() {
        return terracottaConfigUrl;
    }

    /**
     * Set the Terracotta configuration url, to use a TerracottaJobStore. If
     * this is not set, a RamJobStore will be used. Currently not supported.
     */
    @Deprecated
    public void setTerracottaConfigUrl(String terracottaClusterURL) {
        if (terracottaClusterURL != null) {
            throw new UnsupportedOperationException("Scheduled Refresh cannot be used in a clustered manner yet.");
        }
        this.terracottaConfigUrl = terracottaClusterURL;
    }

    /**
     * Fluently, set the Terracotta configuration url, to use a
     * TerracottaJobStore. If this is not set, a RamJobStore will be used.
     * Currently not supported.
     */
    @Deprecated
    public ScheduledRefreshConfiguration terracottaConfigUrl(String terracottaClusterURL) {
        setTerracottaConfigUrl(terracottaClusterURL);
        return this;
    }

    /**
     * Get an additional identifier used in addition to the cache manager and
     * cache name for this extension, and for the job scheduler, and job group.
     * If you are going to have multiple scheduled refresh extensions on the
     * same cache, this is necessary.
     *
     * @return An additional unique identifier for the scheduler and it's jobs
     */
    public String getUniqueNamePart() {
        return uniqueNamePart;
    }

    /**
     * Set an additional identifier used in addition to the cache manager and
     * cache name for this extension, and for the job scheduler, and job group.
     * If you are going to have multiple scheduled refresh extensions on the
     * same cache, this is necessary.
     */
    public void setUniqueNamePart(String part) {
        this.uniqueNamePart = part;
    }

    /**
     * Fluently set an additional identifier used in addition to the cache
     * manager and cache name for this extension, and for the job scheduler, and
     * job group. If you are going to have multiple scheduled refresh extensions
     * on the same cache, this is necessary.
     *
     * @param part unique identifier used to distinguish this scheduled refresh instance
     *             from others on the same cache
     * @return this configuration
     */
    public ScheduledRefreshConfiguration uniqueNamePart(String part) {
        setUniqueNamePart(part);
        return this;
    }

    /**
     * Get whether now value found in all CacheLoaders will force an eviction
     * prematurely from the underlying cache.
     *
     * @return true if refresh will remove keys it annot load through the cache loaders
     */
    public boolean isLoadMissEvicts() {
        return loadMissEvicts;
    }

    /**
     * Set whether now value found in all CacheLoaders will force an eviction
     * prematurely from the underlying cache.
     *
     * @param loadMissEvicts true to evict
     */
    public void setLoadMissEvicts(boolean loadMissEvicts) {
        valid = false;
        this.loadMissEvicts = loadMissEvicts;
    }

    /**
     * Fluently set whether now value found in all CacheLoaders will force an eviction
     * eviction prematurely from the underlying cache.
     *
     * @param loadMissEvicts true to evict
     * @return this configuration
     */
    public ScheduledRefreshConfiguration loadMissEvicts(boolean loadMissEvicts) {
        setLoadMissEvicts(loadMissEvicts);
        return this;
    }

    /**
     * Get the time interval the {@link OverseerJob} will use to poll for job
     * completion.
     *
     * @return time in milliseconds the controlling job will poll the scheduler's
     *         {@link org.quartz.spi.JobStore} in order to schedule the next batch of keys.
     */
    public int getPollTimeMs() {
        return pollTimeMs;
    }

    /**
     * Set the time interval the {@link OverseerJob} will use to poll for job
     * completion.
     *
     * @param pollTimeMs time in milliseconds the controlling job will poll the scheduler's
     *                   {@link org.quartz.spi.JobStore} in order to schedule the next batch of keys.
     */
    public void setPollTimeMs(int pollTimeMs) {
        valid = false;
        this.pollTimeMs = pollTimeMs;
    }

    /**
     * Fluently set the time interval the {@link OverseerJob} will use to poll
     * for job completion.
     *
     * @param pollTimeMs time in milliseconds the controlling job will poll the scheduler's
     *                   {@link org.quartz.spi.JobStore} in order to schedule the next batch of keys.
     * @return this configuration
     */
    public ScheduledRefreshConfiguration pollTimeMs(int pollTimeMs) {
        setPollTimeMs(pollTimeMs);
        return this;
    }

    /**
     * Whether Job misfires (for example if the cluster is down) are fired immediately or
     * wait until their next scheduled time.
     *
     * @return true if refiring will take place immediately
     */
    public boolean isScheduleMisfiresNow() {
        return scheduleMisfiresNow;
    }

    /**
     * Set whether Job misfires (for example if the cluster is down) are fired immediately or
     * wait until their next scheduled time. Currently not supported.
     *
     * @param scheduleMisfiresNow true to schedule misfires immediately
     */
    @Deprecated
    public void setScheduleMisfiresNow(boolean scheduleMisfiresNow) {
        if (scheduleMisfiresNow != DEFAULT_SCHEDULE_MISFIRES_NOW) {
            throw new UnsupportedOperationException("Scheduled Refresh cannot be used in a clustered manner yet; " +
                    "misfire settings are ignored.");
        }

        this.scheduleMisfiresNow = scheduleMisfiresNow;
    }

    /**
     * Set whether Job misfires (for example if the cluster is down) are fired immediately or
     * wait until their next scheduled time. Currently not supported.
     *
     * @param scheduleMisfiresNow true to schedule misfires immediately
     * @return this configuration
     */
    @Deprecated
    public ScheduledRefreshConfiguration scheduleMisfiresNow(boolean scheduleMisfiresNow) {
        this.scheduleMisfiresNow = scheduleMisfiresNow;
        return this;
    }

    /**
     * toString() variant for a specific cache
     *
     * @param targetCache
     * @return
     */
    public String toString(Ehcache targetCache) {
        return "Cache manager: " + targetCache.getCacheManager().getName() + " Cache: " + targetCache.getName() + " " +
                this.toString();
    }

    @Override
    public String toString() {
        return "ScheduledRefreshConfiguration{" +
                "batchSize=" + batchSize +
                ", useBulkload=" + useBulkload +
                ", cronExpression='" + cronExpression + '\'' +
                ", quartzThreadCount=" + quartzThreadCount +
                ", terracottaConfigUrl='" + terracottaConfigUrl + '\'' +
                ", keyGeneratorClass='" + keyGeneratorClass + '\'' +
                ", uniqueNamePart='" + uniqueNamePart + '\'' +
                ", pollTimeMs=" + pollTimeMs +
                ", loadMissEvicts=" + loadMissEvicts +
                ", scheduleMisfiresNow=" + scheduleMisfiresNow +
                ", valid=" + valid +
                '}';
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        ScheduledRefreshConfiguration clone = (ScheduledRefreshConfiguration) super.clone();
        clone.fromProperties(toProperties());
        return clone;
    }
}
