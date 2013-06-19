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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.StatisticsManager;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides a cache extension which allows for the scheduled refresh
 * of all keys currently in the cache, using whichever CacheLoader's are
 * defined. It uses Quartz to do the job scheduling. One extension should be
 * active for a single clustered cache, as multiple extensions will run
 * independently of each other.
 *
 * @author cschanck
 */
public class ScheduledRefreshCacheExtension implements CacheExtension {

   /**
    * Logger this package uses.
    */
   static final Logger LOG = LoggerFactory.getLogger(ScheduledRefreshCacheExtension.class);

   /**
    * Job Property key for Terracotta Job Store to use.
    */
   static final String PROP_QUARTZ_JOB_STORE_TC_CONFIG_URL = "org.quartz.jobStore.tcConfigUrl";

   /**
    * Job Property key for cache name this extension's jobs will run against
    */
   static final String PROP_CACHE_NAME = ScheduledRefreshCacheExtension.class.getName() + ".cacheName";

   /**
    * Job Property key for cache manager name this extension's jobs will run
    * against
    */
   static final String PROP_CACHE_MGR_NAME = ScheduledRefreshCacheExtension.class.getName() + "cacheManagerName";

   /**
    * Job Property key for sending config object to scheduled job
    */
   static final String PROP_CONFIG_OBJECT = ScheduledRefreshCacheExtension.class.getName() + "scheduledRefreshConfig";

   /**
    * Job Property key for sending keys to process to scheduled job
    */
   static final String PROP_KEYS_TO_PROCESS = "keyObjects";

   private static final String OVERSEER_JOB_NAME = "Overseer";
   private Ehcache underlyingCache;
   private ScheduledRefreshConfiguration config;
   private String name;
   private Scheduler scheduler;
   private String groupName;
   private Status status;
   private AtomicLong refreshCount = new AtomicLong();
   private AtomicLong jobCount = new AtomicLong();
   private AtomicLong keysProcessedCount = new AtomicLong();

   /**
    * Constructor. Create an extension with the specified config object against
    * the specified cache.
    *
    * @param config Configuration to use.
    * @param cache  Cache to process against.
    */
   public ScheduledRefreshCacheExtension(ScheduledRefreshConfiguration config, Ehcache cache) {
      if(cache == null) {
         throw new IllegalArgumentException("ScheduledRefresh Cache cannot be null");
      }
      this.underlyingCache = cache;
      if(config == null) {
         throw new IllegalArgumentException("ScheduledRefresh extension config cannot be null");
      }
      this.config = config;
      config.validate();
      this.status = Status.STATUS_UNINITIALISED;
      StatisticsManager.associate(this).withParent(cache);
   }

   @Override
   public void init() {
      try {
         if (config.getScheduledRefreshName() != null) {
            this.name = "scheduledRefresh_" + underlyingCache.getCacheManager().getName() + "_"
                + underlyingCache.getName() + "_" + config.getScheduledRefreshName();
         } else {
            this.name = "scheduledRefresh_" + underlyingCache.getCacheManager().getName() + "_"
                + underlyingCache.getName();
         }
         this.groupName = name + "_grp";
         try {
            makeAndStartQuartzScheduler();
            try {
               scheduleOverseerJob();
               status = Status.STATUS_ALIVE;
            } catch (SchedulerException e) {
               LOG.error("Unable to schedule control job for Scheduled Refresh", e);
            }

         } catch (SchedulerException e) {
            LOG.error("Unable to instantiate Quartz Job Scheduler for Scheduled Refresh", e);
         }
      } catch(RuntimeException e) {
         LOG.error("Unable to initialise ScheduledRefesh extension", e);
      }
   }

   /*
    * This is more complicated at first blush than you might expect, but it
    * attempts to prove that one and only one trigger is scheduled for this job,
    * and that it is the right one. Even if multiple cache extensions for the
    * same cache are sharing the Quartz store, it should end up with only one
    * trigger winning. If there are multiple *different* cron expressions,
    * someone will win, but it is not deterministic as to which one.
    */
   private void scheduleOverseerJob() throws SchedulerException {

      JobDetail seed = makeOverseerJob();

      // build our trigger
      CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(config.getCronExpression());

      CronTrigger trigger = TriggerBuilder.newTrigger()
          .withIdentity(OVERSEER_JOB_NAME, groupName)
          .forJob(seed).withSchedule(cronSchedule)
          .build();

      try {
         scheduler.addJob(seed, false);
      } catch (SchedulerException e) {
         // job already present
      }
      try {
         scheduler.scheduleJob(trigger);
      } catch (SchedulerException e) {
         // trigger already present
         try {
            scheduler.rescheduleJob(trigger.getKey(), trigger);
         } catch (SchedulerException ee) {
            LOG.error("Unable to modify trigger for: " + trigger.getKey());
         }
      }

   }

   /*
    * Add the control job, an instance of the OverseerJob class.
    */
   private JobDetail makeOverseerJob() throws SchedulerException {
      JobDataMap jdm = new JobDataMap();
      jdm.put(PROP_CACHE_MGR_NAME, underlyingCache.getCacheManager().getName());
      jdm.put(PROP_CACHE_NAME, underlyingCache.getName());
      jdm.put(PROP_CONFIG_OBJECT, config);
      JobDetail seed = JobBuilder.newJob(OverseerJob.class).storeDurably()
          .withIdentity(OVERSEER_JOB_NAME, groupName)
          .usingJobData(jdm).build();
      return seed;
   }

   /*
    * Create and start the quartz job scheduler for this cache extension
    */
   private void makeAndStartQuartzScheduler() throws SchedulerException {
      Properties props = new Properties();
      props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, name);
      props.put(StdSchedulerFactory.PROP_SCHED_NAME, name);
      props.put(StdSchedulerFactory.PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON, Boolean.TRUE.toString());
      props.put("org.quartz.threadPool.threadCount", Integer.toString(config.getQuartzThreadCount() + 1));
      Properties jsProps = getJobStoreProperties();
      for (Object key : props.keySet()) {
         if (!jsProps.containsKey(key)) {
            jsProps.put(key, props.get(key));
         }
      }

      StdSchedulerFactory factory = new StdSchedulerFactory(jsProps);
      scheduler = factory.getScheduler();

      scheduler.start();
   }

   private Properties getJobStoreProperties() throws SchedulerException {
      try {
         String clzName = config.getJobStoreFactoryClass();
         Class<?> clz = Class.forName(clzName);
         ScheduledRefreshJobStorePropertiesFactory fact = (ScheduledRefreshJobStorePropertiesFactory) clz.newInstance();
         Properties jsProps = fact.jobStoreProperties(underlyingCache, config);
         return jsProps;
      } catch (Throwable t) {
         throw new SchedulerException(t);
      }
   }

   /**
    * Note that this will not stop other instances of this refresh extension on
    * other nodes (in a clustered environment) from running. Until and unless
    * the last scheduled refresh cache extension for a particular cache/cache
    * manager/unique name is shutdown, they will continue to run. This is only
    * an issue for clustered, TerracottaJobStore-backed scheduled refresh cache
    * extensions.
    *
    * @throws CacheException
    */
   @Override
   public void dispose() throws CacheException {
      try {
         scheduler.shutdown();
      } catch (SchedulerException e) {
         throw new CacheException(e);
      } catch(Throwable t) {
         LOG.info("ScheduledRefresh cache extension exception during shutdown.",t);
      }
      status = Status.STATUS_SHUTDOWN;
   }

   @Override
   public CacheExtension clone(Ehcache cache) throws CloneNotSupportedException {
      return null;
   }

   @Override
   public Status getStatus() {
      return status;
   }

   /**
    * Gets extension group name.
    *
    * @return the extension group name
    */
   String getExtensionGroupName() {
      return groupName;
   }

   /**
    * Gets underlying cache.
    *
    * @return the underlying cache
    */
   Ehcache getUnderlyingCache() {
      return underlyingCache;
   }

   /**
    * Find extension from cache.
    *
    * @param cache the cache
    * @param groupName the group name
    * @return the scheduled refresh cache extension
    */
   static ScheduledRefreshCacheExtension findExtensionFromCache(Ehcache cache, String groupName) {
      for (CacheExtension ce : cache.getRegisteredCacheExtensions()) {
         if (ce instanceof ScheduledRefreshCacheExtension) {
            ScheduledRefreshCacheExtension probe = (ScheduledRefreshCacheExtension) ce;
            if (probe.getUnderlyingCache().getName().equals(cache.getName()) &&
                probe.getExtensionGroupName().equals(groupName)) {
               return probe;
            }
         }
      }
      return null;
   }

   /**
    * Increment refresh count.
    */
   void incrementRefreshCount() {
      refreshCount.incrementAndGet();
   }

   /**
    * Increment job count.
    */
   void incrementJobCount() {
      jobCount.incrementAndGet();
   }

   /**
    * Increment processed count.
    *
    * @param many the many
    */
   void incrementProcessedCount(int many) {
      keysProcessedCount.addAndGet(many);
   }

   /**
    * Gets refresh count.
    *
    * @return the refresh count
    */
   @org.terracotta.statistics.Statistic(name = "refresh", tags = "scheduledrefresh")
   public long getRefreshCount() {
      return refreshCount.get();
   }

   /**
    * Gets job count.
    *
    * @return the job count
    */
   @org.terracotta.statistics.Statistic(name = "job", tags = "scheduledrefresh")
   public long getJobCount() {
      return jobCount.get();
   }

   /**
    * Gets keys processed count.
    *
    * @return the keys processed count
    */
   @org.terracotta.statistics.Statistic(name = "keysprocessed", tags = "scheduledrefresh")
   public long getKeysProcessedCount() {
      return keysProcessedCount.get();
   }

   /**
    * Find refreshed counter statistic. Number of times schedule refresh has been
    * started on this node.
    *
    * @param cache the cache this statistic is attached to.
    * @return the set
    */
   public static Set<ExtendedStatistics.Statistic<Number>> findRefreshStatistics(Ehcache cache) {
      return cache.getStatistics().getExtended().passthru("refresh",
          Collections.singletonMap("scheduledrefresh", null).keySet());
   }

   /**
    * Find job counter statistic. Number of batch jobs executed on this node.
    *
    * @param cache the cache this statistic is attached to.
    * @return the set
    */
   public static Set<ExtendedStatistics.Statistic<Number>> findJobStatistics(Ehcache cache) {
      return cache.getStatistics().getExtended().passthru("job",
          Collections.singletonMap("scheduledrefresh", null).keySet());
   }

   /**
    * Find queued counter statistic. Number of batch jobs executed on this node.
    *
    * @param cache the cache this statistic is attached to.
    * @return the set
    */
   public static Set<ExtendedStatistics.Statistic<Number>> findKeysProcessedStatistics(Ehcache cache) {
      return cache.getStatistics().getExtended().passthru("keysprocessed",
          Collections.singletonMap("scheduledrefresh", null).keySet());
   }

   /**
    * Finds a single refresh statistic for this cache. This is the count of scheduled
    * refresh invocations for this node. Throws {@link IllegalStateException} if
    * there are none or more than one.
    *
    * @param cache the cache
    * @return the extended statistics . statistic
    */
   public static ExtendedStatistics.Statistic<Number> findRefreshStatistic(Ehcache cache) {
      Set<ExtendedStatistics.Statistic<Number>> set = findRefreshStatistics(cache);
      if (set.size() == 1) {
         return set.iterator().next();
      } else {
         throw new IllegalStateException("Multiple scheduled refresh stats found for this cache");
      }
   }

   /**
    * Finds a single job statistic for this cache. This is the count of refresh batch jobs
    * executed on this node. Throws {@link IllegalStateException} if there are none or
    * more than one.
    *
    * @param cache the cache
    * @return the extended statistics . statistic
    */
   public static ExtendedStatistics.Statistic<Number> findJobStatistic(Ehcache cache) {
      Set<ExtendedStatistics.Statistic<Number>> set = findJobStatistics(cache);
      if (set.size() == 1) {
         return set.iterator().next();
      } else {
         throw new IllegalStateException("Multiple scheduled refresh stats found for this cache");
      }
   }

   /**
    * Finds a single keys processed statistic for this cache. This is the count of keys
    * refreshed on this node. Throws {@link IllegalStateException} if
    * there are none or more than one.
    *
    * @param cache the cache
    * @return the extended statistics . statistic
    */
   public static ExtendedStatistics.Statistic<Number> findKeysProcessedStatistic(Ehcache cache) {
      Set<ExtendedStatistics.Statistic<Number>> set = findKeysProcessedStatistics(cache);
      if (set.size() == 1) {
         return set.iterator().next();
      } else {
         throw new IllegalStateException("Multiple scheduled refresh stats found for this cache");
      }
   }

}
