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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the seed job for a scheduled execution of a scheduled refresh job.
 * This job will generate keys and batch them up, scheduling jobs, until the
 * complete set of keys is finished. This job will remain running as all the
 * other jobs run, and is responsible for starting all the individual refresh
 * jobs, enabling bulk load mode beforehand, and disabling bulk load mode
 * afterwards.
 *
 * @author cschanck
 */
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class OverseerJob implements Job {
   private static final Logger LOG = LoggerFactory.getLogger(OverseerJob.class);

   private static final AtomicLong INSTANCE_ID_GENERATOR = new AtomicLong(0);

   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
         JobDataMap jdm = context.getMergedJobDataMap();
         ScheduledRefreshConfiguration config = (ScheduledRefreshConfiguration) jdm
             .get(ScheduledRefreshCacheExtension.PROP_CONFIG_OBJECT);
         String cacheManagerName = jdm.getString(ScheduledRefreshCacheExtension.PROP_CACHE_MGR_NAME);
         String cacheName = jdm.getString(ScheduledRefreshCacheExtension.PROP_CACHE_NAME);

         final CacheManager cacheManager = CacheManager.getCacheManager(cacheManagerName);

         if (cacheManager == null) {
            LOG.warn("Unable to process Scheduled Refresh batch" + context.getJobDetail().getKey() + ": cache "
                + "manager " + cacheManager + " not found");
            return;
         }

         final Ehcache cache = cacheManager.getEhcache(cacheName);
         if (cache == null) {
            LOG.warn("Unable to process Scheduled Refresh batch" + context.getJobDetail().getKey() + ": cache "
                + cacheName + " not found");
            return;
         }

         ScheduledRefreshCacheExtension extension = ScheduledRefreshCacheExtension.findExtensionFromCache(cache,
             context.getJobDetail().getKey().getGroup());
         boolean keepingStats = false;
         if (extension != null) {
            keepingStats = true;
            extension.incrementRefreshCount();
         }

         ScheduledRefreshKeyGenerator<Serializable> generator = makeGeneratorObject(config.getKeyGeneratorClass());
         if (generator != null) {
            Scheduler scheduler = context.getScheduler();
            // if we are the only ones running...

            LOG.info("Starting Scheduled refresh: " + context.getJobDetail().getKey()+" "+statsNote(keepingStats));
            processKeys(context, config, cache, generator);
            if (config.isUseBulkload()) {
               try {
                  waitForOutstandingJobCount(context, config, scheduler, 0);
               } catch (SchedulerException e) {
                  LOG.warn(
                      "Unable to process Scheduled Refresh batch termination" + context.getJobDetail().getKey(), e);
               }
            }
         }

      } catch (Throwable e) {
         try {
            if (!context.getScheduler().isShutdown()) {
               LOG.warn("Unable to process Scheduled Refresh batch " + context.getJobDetail().getKey(), e);
            }
         } catch (SchedulerException e1) {
            LOG.warn(e1.getMessage(), e1);
         }
      }
   }

   static String statsNote(boolean keepingStats) {
      return "["+(keepingStats?"with stats":"no stats")+"]";
   }

   private int getOutstandingJobCount(JobExecutionContext context, Scheduler scheduler) throws SchedulerException {
      GroupMatcher<JobKey> matcher = GroupMatcher.jobGroupEquals(context.getJobDetail().getKey().getGroup());
      Set<JobKey> queuedKeys = scheduler.getJobKeys(matcher);
      return queuedKeys.size();
   }

   private void waitForOutstandingJobCount(JobExecutionContext context, ScheduledRefreshConfiguration config,
                                           Scheduler scheduler, int minCount) throws SchedulerException {
      GroupMatcher<JobKey> matcher = GroupMatcher.jobGroupEquals(context.getJobDetail().getKey().getGroup());
      for (Set<JobKey> queuedKeys = scheduler.getJobKeys(matcher); (!scheduler.isShutdown())
          && (queuedKeys.size() > minCount); queuedKeys = scheduler.getJobKeys(matcher)) {
         try {
            Thread.sleep(config.getPollTimeMs());
         } catch (InterruptedException e) {
         }
      }
   }

   private void processKeys(JobExecutionContext context, ScheduledRefreshConfiguration config, final Ehcache cache,
                            ScheduledRefreshKeyGenerator<Serializable> generator) throws JobExecutionException {
      ArrayList<Serializable> batch = new ArrayList<Serializable>(config.getBatchSize());
      for (Serializable key : generator.generateKeys(cache)) {
         batch.add(key);
         if (batch.size() >= config.getBatchSize()) {
            try {
               process(context, cache, config, batch);
               batch = new ArrayList<Serializable>();
            } catch (SchedulerException e) {
               LOG.warn("Unable to process Scheduled Refresh batch" + context.getJobDetail().getKey(), e);
               throw new JobExecutionException(e);
            }
            batch.clear();
         }
      }
      if (!batch.isEmpty()) {
         try {
            process(context, cache, config, batch);
         } catch (SchedulerException e) {
            LOG.warn("Unable to process Scheduled Refresh batch" + context.getJobDetail().getKey(), e);
            throw new JobExecutionException(e);
         }
      }
   }

   private void process(JobExecutionContext context, Ehcache underlyingCache, ScheduledRefreshConfiguration config,
                        List<Serializable> batch) throws SchedulerException {

      JobDataMap map = new JobDataMap(context.getJobDetail().getJobDataMap());

      map.put(ScheduledRefreshCacheExtension.PROP_KEYS_TO_PROCESS, batch);

      Scheduler scheduler = context.getScheduler();

      JobDetail job = JobBuilder
          .newJob(RefreshBatchJob.class)
          .withIdentity("RefreshBatch-" + INSTANCE_ID_GENERATOR.incrementAndGet(),
              context.getTrigger().getJobKey().getGroup()).usingJobData(map).build();

      try {
         waitForOutstandingJobCount(context, config, scheduler, config.getParallelJobCount());

         if (!scheduler.isShutdown()) {

            Trigger trigger = TriggerBuilder.newTrigger().startNow().forJob(job).build();
            scheduler.scheduleJob(job, trigger);

         }
      } catch (SchedulerException e) {
         if (!scheduler.isShutdown()) {
            throw e;
         }
      }
   }

   private ScheduledRefreshKeyGenerator<Serializable> makeGeneratorObject(String keyGeneratorClass) {
      try {
         Class<?> gen = Class.forName(keyGeneratorClass);
         @SuppressWarnings("unchecked")
         ScheduledRefreshKeyGenerator<Serializable> obj = (ScheduledRefreshKeyGenerator<Serializable>) gen
             .newInstance();
         return obj;
      } catch (ClassNotFoundException e) {
         LOG.warn("Unable to instantiate key generator class: " + keyGeneratorClass, e);
      } catch (InstantiationException e) {
         LOG.warn("Unable to instantiate key generator class: " + keyGeneratorClass, e);
      } catch (IllegalAccessException e) {
         LOG.warn("Unable to instantiate key generator class: " + keyGeneratorClass, e);
      }
      return null;
   }

}
