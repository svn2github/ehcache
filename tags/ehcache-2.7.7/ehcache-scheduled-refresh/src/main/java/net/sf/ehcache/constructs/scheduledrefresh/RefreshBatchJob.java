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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.loader.CacheLoader;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used to actually process a batch of keys for refreshing.
 * Instances of this job are scheduled by the {@link OverseerJob} class.
 *
 * @author cschanck
 */
public class RefreshBatchJob implements Job {

   private static final Logger LOG = LoggerFactory.getLogger(OverseerJob.class);
   private static HashMap<String, AtomicInteger> bulkLoadTrackingMap = new HashMap<String, AtomicInteger>(1);
   private static ReentrantLock bulkloadLock = new ReentrantLock();

   private static void requestBulkLoadEnabled(Ehcache cache) {
      bulkloadLock.lock();
      try {
         boolean prior = cache.isNodeBulkLoadEnabled();
         // yes, this is racy. we can do no better until we have per-thread bulk loading
         if (prior) {
            String key = cache.getCacheManager().getName() + "/" + cache.getName();
            AtomicInteger permits = bulkLoadTrackingMap.get(key);
            if (permits == null) {
               // first time in. actually switch it
               permits = new AtomicInteger(1);
               bulkLoadTrackingMap.put(key, permits);
               cache.setNodeBulkLoadEnabled(true);
            } else {
               permits.incrementAndGet();
            }
         }
      } finally {
         bulkloadLock.unlock();
      }
   }

   private static void requestBulkLoadRestored(Ehcache cache) {
      bulkloadLock.lock();
      try {
         String key = cache.getCacheManager().getName() + "/" + cache.getName();
         AtomicInteger permits = bulkLoadTrackingMap.get(key);
         if (permits != null) {
            if (permits.decrementAndGet() == 0) {
               // last one out. reset it to true
               bulkLoadTrackingMap.remove(key);
               cache.setNodeBulkLoadEnabled(true);
            }
         }
      } finally {
         bulkloadLock.unlock();
      }
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
         JobDataMap jdm = context.getMergedJobDataMap();
         ScheduledRefreshConfiguration config = (ScheduledRefreshConfiguration) jdm.get(ScheduledRefreshCacheExtension
             .PROP_CONFIG_OBJECT);
         String cacheManagerName = jdm.getString(ScheduledRefreshCacheExtension.PROP_CACHE_MGR_NAME);
         String cacheName = jdm.getString(ScheduledRefreshCacheExtension.PROP_CACHE_NAME);

         CacheManager cacheManager = CacheManager.getCacheManager(cacheManagerName);
         Ehcache underlyingCache = cacheManager.getEhcache(cacheName);

         HashSet<? extends Object> keysToProcess = new HashSet((Collection<? extends Object>) jdm.get(
             ScheduledRefreshCacheExtension.PROP_KEYS_TO_PROCESS));

         ScheduledRefreshCacheExtension extension = ScheduledRefreshCacheExtension.findExtensionFromCache(underlyingCache,
             context.getJobDetail().getKey().getGroup());
         boolean keepingStats=false;
         if (extension != null) {
            extension.incrementJobCount();
            extension.incrementProcessedCount(keysToProcess.size());
            keepingStats=true;
         }

         LOG.info("Scheduled refresh batch job: " + context.getJobDetail().getKey() + " size: " + keysToProcess.size()+" "+OverseerJob.statsNote(keepingStats));
         try {
            if (config.isUseBulkload()) {
               requestBulkLoadEnabled(underlyingCache);
            }
         } catch (UnsupportedOperationException e) {
            LOG.warn("Bulk Load requested for cache that does not support bulk load.");
         }

         // iterate through the loaders
         for (CacheLoader loader : underlyingCache.getRegisteredCacheLoaders()) {
            // if we are out of keys, punt
            if (keysToProcess.isEmpty()) {
               break;
            }

            // try and load them all
            Map<? extends Object, ? extends Object> values = loader.loadAll(keysToProcess);
            // subtract the ones that were loaded
            keysToProcess.removeAll(values.keySet());
            for (Map.Entry<? extends Object, ? extends Object> entry : values.entrySet()) {
               Element newElement = new Element(entry.getKey(), entry.getValue());
               underlyingCache.put(newElement);
            }
         }
         // assume we got here ok, now evict any that don't evict
         if (config.isEvictOnLoadMiss() && !keysToProcess.isEmpty()) {
            underlyingCache.removeAll(keysToProcess);
         }

         try {
            if (config.isUseBulkload()) {
               requestBulkLoadRestored(underlyingCache);
            }
         } catch (UnsupportedOperationException e) {
            // warned above.
         }
      }
      catch(Throwable t) {
         LOG.warn("Scheduled refresh batch job failure: " + context.getJobDetail().getKey(), t);
      }

   }

}
