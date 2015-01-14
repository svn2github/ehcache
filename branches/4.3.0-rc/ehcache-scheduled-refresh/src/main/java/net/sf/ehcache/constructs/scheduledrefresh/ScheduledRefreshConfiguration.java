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
 * initialized from a {@link Properties} object. Currently, the use of a
 * clustered {@link org.terracotta.quartz.TerracottaJobStore} is not supported.
 * This usage will be supported in the future.
 *
 * @author cschanck
 */
public class ScheduledRefreshConfiguration implements Serializable, Cloneable {

   /**
    * Property keys for configuration.
    */
   public enum PropKey {
      /**
       * Properties key for the batch size attribute.
       */
      batchSize,

      /**
       * Properties key for the key generator class name.
       */
      keyGenerator,


      /**
       * Properties key for cron expression used to schedule this job.
       */
      cronExpression,

      /**
       * Properties key for enabling bulk load mode prior to exection of the
       * refresh.
       */
      useBulkload,

      /**
       * Properties key for the quartz job count attribute.
       */
      quartzJobCount,

      /**
       * Properties key for the terracotta config url.
       */
      tcConfigUrl,

      /**
       * Properties key for the unique name identifier.
       */
      scheduledRefreshName,

      /**
       * Properties key for the seed job polling interval.
       */
      pollTimeMs,

      /**
       * Properties key for evictions on refresh fail.
       */
      evictOnLoadMiss,

      /**
       * Properties key for the job store factory.
       */
      jobStoreFactory,

      /**
       * Properties key for the job store factory.
       */
      parallelJobCount,
   }

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
    * Default simultaneous Quartz thread count per node.
    */
   public static final int DEFAULT_QUARTZ_THREADCOUNT = 2;

   /**
    * Default number of in process job count over the entire cluster.
    */
   public static final int DEFAULT_PARALLEL_JOB_COUNT = DEFAULT_QUARTZ_THREADCOUNT;

   /**
    * Default polling timeout for monitoring refresh jobs.
    */
   public static final int DEFAULT_POLL_TIME_MS = (int) TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS);

   /**
    * The Constant DEFAULT_KEY_GENERATOR_CLASS.
    */
   private static final String DEFAULT_KEY_GENERATOR_CLASS = SimpleScheduledRefreshKeyGenerator.class.getName();

   /**
    * The Constant DEFAULT_JOB_STORE_FACTORY_CLASS.
    */
   private static final String DEFAULT_JOB_STORE_FACTORY_CLASS = ScheduledRefreshRAMJobStoreFactory.class.getName();

   /**
    * The Constant serialVersionUID.
    */
   private static final long serialVersionUID = -6877036694574988955L;

   /**
    * The batch size.
    */
   private int batchSize = DEFAULT_BATCHSIZE;

   /**
    * The use bulkload.
    */
   private boolean useBulkload = DEFAULT_USE_BULKLOAD;

   /**
    * The cron expression.
    */
   private String cronExpression = null;

   /**
    * The quartz thread count.
    */
   private int quartzThreadCount = DEFAULT_QUARTZ_THREADCOUNT;

   /**
    * Default parallel job count
    */
   private int parallelJobCount = DEFAULT_PARALLEL_JOB_COUNT;

   /**
    * The key generator class.
    */
   private String keyGeneratorClass = DEFAULT_KEY_GENERATOR_CLASS;

   /**
    * The unique name part.
    */
   private String scheduledRefreshName = null;

   /**
    * The job store factory class name.
    */
   private String jobStoreFactoryClassName = DEFAULT_JOB_STORE_FACTORY_CLASS;

   /**
    * the terracootta config url
    */
   private String tcConfigUrl = null;

   /**
    * The poll time ms.
    */
   private int pollTimeMs = DEFAULT_POLL_TIME_MS;

   /**
    * The load miss evicts.
    */
   private boolean evictOnLoadMiss = DEFAULT_NULL_EVICTS;

   /**
    * The valid.
    */
   private volatile boolean valid = false;

   /**
    * Excess properties passed to the extension
    */
   private Properties excessProperties = new Properties();

   /**
    * Frozen flag. Once build() is called, the config is frozen.
    */
   private volatile boolean frozen = false;

   /**
    * Create a default, valid configuration.
    */
   public ScheduledRefreshConfiguration() {
   }

   /**
    * Initialize this configuration from a {@link Properties} object. Will be
    * validated before returning.
    *
    * @param properties the properties
    * @return this configuration
    */
   public ScheduledRefreshConfiguration fromProperties(Properties properties) {
      valid = false;
      frozen = false;
      excessProperties.clear();
      if (properties != null) {
         for (String property : properties.stringPropertyNames()) {
            String stringValue = properties.getProperty(property).trim();
            PropKey pk;
            try {
               pk = PropKey.valueOf(property);
            } catch (Exception e) {
               pk = null;
            }
            if (pk != null) {
               switch (pk) {
                  case batchSize:
                     setBatchSize(Integer.parseInt(stringValue));
                     break;
                  case useBulkload:
                     setUseBulkload(Boolean.parseBoolean(stringValue));
                     break;
                  case cronExpression:
                     setCronExpression(stringValue);
                     break;
                  case jobStoreFactory:
                     setJobStoreFactoryClassName(stringValue);
                     break;
                  case quartzJobCount:
                     setQuartzThreadCount(Integer.parseInt(stringValue));
                     break;
                  case parallelJobCount:
                     setParallelJobCount(Integer.parseInt(stringValue));
                     break;
                  case pollTimeMs:
                     setPollTimeMs(Integer.parseInt(stringValue));
                     break;
                  case evictOnLoadMiss:
                     setEvictOnLoadMiss(Boolean.parseBoolean(stringValue));
                     break;
                  case tcConfigUrl:
                     setTerracottaConfigUrl(stringValue);
                     break;
                  case keyGenerator:
                     setKeyGeneratorClass(stringValue);
                     break;
                  default:
                     throw new IllegalStateException("Unhandled property key: " + pk);
               }
            } else {
               excessProperties.put(property, stringValue);
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
      p.setProperty(PropKey.batchSize.name(), Long.toString(getBatchSize()));
      p.setProperty(PropKey.useBulkload.name(), Boolean.toString(isUseBulkload()));
      p.setProperty(PropKey.evictOnLoadMiss.name(), Boolean.toString(isEvictOnLoadMiss()));
      p.setProperty(PropKey.cronExpression.name(), getCronExpression());
      p.setProperty(PropKey.jobStoreFactory.name(), getJobStoreFactoryClass());
      p.setProperty(PropKey.quartzJobCount.name(), Integer.toString(getQuartzThreadCount()));
      p.setProperty(PropKey.parallelJobCount.name(), Integer.toString(getParallelJobCount()));
      p.setProperty(PropKey.pollTimeMs.name(), Integer.toString(getPollTimeMs()));
      p.setProperty(PropKey.keyGenerator.name(), getKeyGeneratorClass());
      p.setProperty(PropKey.tcConfigUrl.name(), getTerracottaConfigUrl());
      for (String property : excessProperties.stringPropertyNames()) {
         String stringValue = excessProperties.getProperty(property).trim();
         p.put(property, stringValue);
      }
      return p;
   }

   /**
    * Validate and mark this configuration good to use.
    *
    * @return validated configuration
    */
   public ScheduledRefreshConfiguration build() {
      validate();
      frozen=true;
      return this;
   }

   private void checkFrozen() {
      if(frozen) {
         throw new IllegalStateException("Can't modify a frozen configuration.");
      }
   }
   /**
    * Validate this configuration.
    */
   public void validate() {
      if (cronExpression == null) {
         throw new IllegalArgumentException("Cron Schedule cannot be unspecified");
      }
      if(parallelJobCount<2) {
         throw new IllegalArgumentException("parallelJobCount must be >= 2 ["+parallelJobCount+"]");
      }
      if(quartzThreadCount<2) {
         throw new IllegalArgumentException("quartzThreadCount must be >= 2 ["+quartzThreadCount+"]");
      }
      if(batchSize<1) {
         throw new IllegalArgumentException("batchSize must be >= 1 ["+batchSize+"]");
      }
      if(pollTimeMs<0) {
         throw new IllegalArgumentException("pollTimeMS must be >=0 ["+pollTimeMs+"]");
      }
      final long oneMinuteMS=TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);
      if(pollTimeMs>oneMinuteMS) {
         throw new IllegalArgumentException("pollTimeMS must be < "+oneMinuteMS+" ["+pollTimeMs+"]");
      }

      if (jobStoreFactoryClassName == null) {
         jobStoreFactoryClassName = DEFAULT_JOB_STORE_FACTORY_CLASS;
      }
      if (keyGeneratorClass == null) {
         keyGeneratorClass = DEFAULT_KEY_GENERATOR_CLASS;
      }
      valid = true;
   }

   /**
    * is this configuration valid to use?.
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
      checkFrozen();
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
    * @param useBulkload the new use bulkload
    */
   public void setUseBulkload(boolean useBulkload) {
      checkFrozen();
      valid = false;
      this.useBulkload = useBulkload;
   }

   /**
    * Fluently set the bulk load flag.
    *
    * @param yes the yes
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
    * @param cronExpression the new cron expression
    */
   public void setCronExpression(String cronExpression) {
      checkFrozen();
      valid = false;
      this.cronExpression = cronExpression;
   }

   /**
    * Fluently set the cron expression Quartz will use for scheduling this
    * refresh job.
    *
    * @param cronExpression the cron expression
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
    * @param quartzThreadCount the new quartz thread count
    */
   public void setQuartzThreadCount(int quartzThreadCount) {
      checkFrozen();
      valid = false;
      this.quartzThreadCount = quartzThreadCount;
   }

   /**
    * Fluently set the Quartz thread count.
    *
    * @param quartzThreadCount the quartz thread count
    * @return this configuration
    */
   public ScheduledRefreshConfiguration quartzThreadCount(int quartzThreadCount) {
      setQuartzThreadCount(quartzThreadCount);
      return this;
   }

   /**
    * Get the key generator class used to generate the list of keys to refresh.
    *
    * @return the fully qualified class name of the
    *         {@link ScheduledRefreshKeyGenerator} class
    */
   public String getKeyGeneratorClass() {
      return keyGeneratorClass;
   }

   /**
    * Set the key generator class used to generate the list of keys to refresh.
    * This is the class used to generate keys from the target cache. A simple
    * implementation of the naive getKeys() approach is supplied.
    *
    * @param keyGeneratorClass the new key generator class
    */
   public void setKeyGeneratorClass(String keyGeneratorClass) {
      checkFrozen();
      this.keyGeneratorClass = keyGeneratorClass;
   }

   /**
    * Fluently set the key generator class used to generate the list of keys to
    * refresh.
    *
    * @param keyGeneratorClass the key generator class
    * @return this configuration
    */
   public ScheduledRefreshConfiguration keyGeneratorClass(String keyGeneratorClass) {
      setKeyGeneratorClass(keyGeneratorClass);
      return this;
   }

   /**
    * Get an additional identifier used in addition to the cache manager and
    * cache name for this extension, and for the job scheduler, and job group.
    * If you are going to have multiple scheduled refresh extensions on the same
    * cache, this is necessary.
    *
    * @return An additional unique identifier for the scheduler and it's jobs
    */
   public String getScheduledRefreshName() {
      return scheduledRefreshName;
   }

   /**
    * Set an additional identifier used in addition to the cache manager and
    * cache name for this extension, and for the job scheduler, and job group.
    * If you are going to have multiple scheduled refresh extensions on the same
    * cache, this is necessary.
    *
    * @param part the new unique name part
    */
   public void setScheduledRefreshName(String part) {
      checkFrozen();
      this.scheduledRefreshName = part;
   }

   /**
    * Fluently set an additional identifier used in addition to the cache
    * manager and cache name for this extension, and for the job scheduler, and
    * job group. If you are going to have multiple scheduled refresh extensions
    * on the same cache, this is necessary.
    *
    * @param part unique identifier used to distinguish this scheduled refresh
    *             instance from others on the same cache
    * @return this configuration
    */
   public ScheduledRefreshConfiguration scheduledRefreshName(String part) {
      setScheduledRefreshName(part);
      return this;
   }

   /**
    * Get whether now value found in all CacheLoaders will force an eviction
    * prematurely from the underlying cache.
    *
    * @return true if refresh will remove keys it annot load through the cache
    *         loaders
    */
   public boolean isEvictOnLoadMiss() {
      return evictOnLoadMiss;
   }

   /**
    * Set whether now value found in all CacheLoaders will force an eviction
    * prematurely from the underlying cache.
    *
    * @param loadMissEvicts true to evict
    */
   public void setEvictOnLoadMiss(boolean loadMissEvicts) {
      checkFrozen();
      valid = false;
      this.evictOnLoadMiss = loadMissEvicts;
   }

   /**
    * Fluently set whether now value found in all CacheLoaders will force an
    * eviction eviction prematurely from the underlying cache.
    *
    * @param loadMissEvicts true to evict
    * @return this configuration
    */
   public ScheduledRefreshConfiguration evictOnLoadMiss(boolean loadMissEvicts) {
      setEvictOnLoadMiss(loadMissEvicts);
      return this;
   }

   /**
    * Get the time interval the {@link OverseerJob} will use to poll for job
    * completion.
    *
    * @return time in milliseconds the controlling job will poll the scheduler's
    *         {@link org.quartz.spi.JobStore} in order to schedule the next
    *         batch of keys.
    */
   public int getPollTimeMs() {
      return pollTimeMs;
   }

   /**
    * Set the time interval the {@link OverseerJob} will use to poll for job
    * completion.
    *
    * @param pollTimeMs time in milliseconds the controlling job will poll the
    *                   scheduler's {@link org.quartz.spi.JobStore} in order to schedule
    *                   the next batch of keys.
    */
   public void setPollTimeMs(int pollTimeMs) {
      checkFrozen();
      valid = false;
      this.pollTimeMs = pollTimeMs;
   }

   /**
    * Fluently set the time interval the {@link OverseerJob} will use to poll
    * for job completion.
    *
    * @param pollTimeMs time in milliseconds the controlling job will poll the
    *                   scheduler's
    * @return this configuration {@link org.quartz.spi.JobStore} in order to
    *         schedule the next batch of keys.
    */
   public ScheduledRefreshConfiguration pollTimeMs(int pollTimeMs) {
      setPollTimeMs(pollTimeMs);
      return this;
   }

   /**
    * Gets the job store factory class.
    *
    * @return the job store factory class
    */
   public String getJobStoreFactoryClass() {
      return jobStoreFactoryClassName;
   }

   /**
    * Sets the job store factory class name.
    *
    * @param className the new job store factory class name
    */
   public void setJobStoreFactoryClassName(String className) {
      checkFrozen();
      this.jobStoreFactoryClassName = className;
   }

   /**
    * Fluently set the Job store factory.
    *
    * @param className the class name
    * @return the scheduled refresh configuration
    */
   public ScheduledRefreshConfiguration jobStoreFactory(String className) {
      setJobStoreFactoryClassName(className);
      return this;
   }

   /**
    * Get any unrecognized properties that were passed to this config when
    * constructed via a Properties object.
    *
    * @return properties
    */
   public Properties getExcessProperties() {
      return excessProperties;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ScheduledRefreshConfiguration{" + "terracottaConfigUrl=" + getTerracottaConfigUrl() +
          ", batchSize=" + batchSize + ", useBulkload=" + useBulkload
          + ", cronExpression='" + cronExpression + '\'' + ", quartzThreadCount=" + quartzThreadCount
          + ", parallelJobCount=" + parallelJobCount
          + ", keyGeneratorClass='" + keyGeneratorClass + '\'' + ", uniqueNamePart='" + scheduledRefreshName + '\''
          + ", pollTimeMs=" + pollTimeMs + ", loadMissEvicts=" + evictOnLoadMiss + ", valid=" + valid + '}';
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#clone()
    */
   @Override
   protected Object clone() throws CloneNotSupportedException {
      ScheduledRefreshConfiguration clone = (ScheduledRefreshConfiguration) super.clone();
      clone.fromProperties(toProperties());
      return clone;
   }

   /**
    * Sets terracotta config url. Also sets (or clears) the job store factory
    * to {@link ScheduledRefreshTerracottaJobStoreFactory} or null.
    *
    * @param terracottaConfigUrl the terracotta config url
    */
   public void setTerracottaConfigUrl(String terracottaConfigUrl) {
      checkFrozen();
      valid = false;
      this.tcConfigUrl = terracottaConfigUrl;
      if (terracottaConfigUrl == null) {
         setJobStoreFactoryClassName(null);
      } else {
         setJobStoreFactoryClassName(ScheduledRefreshTerracottaJobStoreFactory.class.getName());
      }
   }

   /**
    * Set the Terracotta config url, fluently.
    *
    * @param terracottaConfigUrl the terracotta config url
    * @return the scheduled refresh configuration
    */
   public ScheduledRefreshConfiguration terracottaConfigUrl(String terracottaConfigUrl) {
      setTerracottaConfigUrl(terracottaConfigUrl);
      return this;
   }

   /**
    * Gets terracotta config url.
    *
    * @return the terracotta config url
    */
   public String getTerracottaConfigUrl() {
      return tcConfigUrl;
   }

   /**
    * Sets parallel job count.
    *
    * @param parallelJobCount the parallel job count
    */
   public void setParallelJobCount(int parallelJobCount) {
      checkFrozen();
      this.parallelJobCount = parallelJobCount;
      valid = false;
   }

   /**
    * Parallel job count.
    *
    * @param parallelJobCount the parallel job count
    * @return the scheduled refresh configuration
    */
   public ScheduledRefreshConfiguration parallelJobCount(int parallelJobCount) {
      setParallelJobCount(parallelJobCount);
      return this;
   }

   /**
    * Gets parallel job count.
    *
    * @return the parallel job count
    */
   public int getParallelJobCount() {
      return parallelJobCount;
   }

}
