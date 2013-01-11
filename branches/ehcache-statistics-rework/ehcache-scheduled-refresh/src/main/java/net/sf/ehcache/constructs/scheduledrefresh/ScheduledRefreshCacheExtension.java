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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.extension.CacheExtension;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

   private Ehcache underlyingCache;
   private ScheduledRefreshConfiguration config;
   private String name;
   private Scheduler scheduler;
   private String groupName;
   private Status status;

   /**
    * Constructor. Create an extension with the specified config object against
    * the specified cache.
    * 
    * @param config
    *           Configuration to use.
    * @param cache
    *           Cache to process against.
    */
   public ScheduledRefreshCacheExtension(ScheduledRefreshConfiguration config, Ehcache cache) {
      this.underlyingCache = cache;
      this.config = config;
      this.status = Status.STATUS_UNINITIALISED;
   }

   @Override
   public void init() {
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
            JobDetail seed = addOverseerJob();

            try {
               scheduleOverseerJob(seed);
               status = Status.STATUS_ALIVE;
            } catch (SchedulerException e) {
               LOG.error("Unable to schedule control job for Scheduled Refresh", e);
            }

         } catch (SchedulerException e) {
            LOG.error("Unable to add Scheduled Refresh control job to Quartz Job Scheduler", e);
         }
      } catch (SchedulerException e) {
         LOG.error("Unable to instantiate Quartz Job Scheduler for Scheduled Refresh", e);
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
   private void scheduleOverseerJob(JobDetail seed) throws SchedulerException {

      // build our trigger
      CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(config.getCronExpression());

      CronTrigger trigger = TriggerBuilder.newTrigger().forJob(seed).withSchedule(cronSchedule).build();

      // delete existing triggers. Remember the job is durable, so no problem
      // here.
      List<? extends Trigger> triggers = scheduler.getTriggersOfJob(seed.getKey());

      if (triggers.size() >= 1) {
         List<TriggerKey> triggerKeys = new ArrayList<TriggerKey>();
         for (Trigger t : triggers) {
            triggerKeys.add(t.getKey());
         }

         scheduler.unscheduleJobs(triggerKeys);
      }

      // now, keep trying to add and count the trigger, until there is only 1
      // trigger there.
      boolean done = false;
      int retryCount = 0;
      do {
         // schedule ours
         scheduler.scheduleJob(trigger);
         if (retryCount++ >= 1) {
            LOG.info("Scheduled Refresh retry [" + retryCount + "] for " + config);
         }

         // see if there is only 1 job in there after we have added ours.
         // if so, we are happy.
         triggers = scheduler.getTriggersOfJob(seed.getKey());
         if (triggers.size() > 1) {
            List<TriggerKey> triggerKeys = new ArrayList<TriggerKey>();
            for (Trigger t : triggers) {
               triggerKeys.add(t.getKey());
            }

            scheduler.unscheduleJobs(triggerKeys);
         } else {
            // if only 1 was found, all done
            done = true;
         }
      } while (!done);

   }

   /*
    * Add the control job, an instance of the OverseerJob class.
    */
   private JobDetail addOverseerJob() throws SchedulerException {
      JobDataMap jdm = new JobDataMap();
      jdm.put(PROP_CACHE_MGR_NAME, underlyingCache.getCacheManager().getName());
      jdm.put(PROP_CACHE_NAME, underlyingCache.getName());
      jdm.put(PROP_CONFIG_OBJECT, config);
      JobDetail seed = JobBuilder.newJob(OverseerJob.class).storeDurably().withIdentity("seed", groupName)
            .usingJobData(jdm).build();
      scheduler.addJob(seed, true);
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
         if (!props.containsKey(key)) {
            props.put(key, jsProps.get(key));
         }
      }

      StdSchedulerFactory factory = new StdSchedulerFactory(props);
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

}
