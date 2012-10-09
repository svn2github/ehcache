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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.loader.CacheLoader;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This class is used to actually process a batch of keys for refreshing.
 * Instances of this job are scheduled by the {@link OverseerJob} class.
 * 
 * @author cschanck
 * 
 */
public class RefreshBatchJob implements Job {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        JobDataMap jdm = context.getMergedJobDataMap();
        ScheduledRefreshConfiguration config = (ScheduledRefreshConfiguration) jdm
                .get(ScheduledRefreshCacheExtension.PROP_CONFIG_OBJECT);
        String cacheManagerName = jdm
                .getString(ScheduledRefreshCacheExtension.PROP_CACHE_MGR_NAME);
        String cacheName = jdm
                .getString(ScheduledRefreshCacheExtension.PROP_CACHE_NAME);

        CacheManager cacheManager = CacheManager
                .getCacheManager(cacheManagerName);
        Cache underlyingCache = cacheManager.getCache(cacheName);

        HashSet<? extends Object> keysToProcess = new HashSet(
                (Collection<? extends Object>) jdm
                        .get(ScheduledRefreshCacheExtension.PROP_KEYS_TO_PROCESS));

        // iterate through the loaders
        for (CacheLoader loader : underlyingCache.getRegisteredCacheLoaders()) {
            // if we are out of keys, punt
            if (keysToProcess.isEmpty()) {
                break;
            }

            // try and load them all
            Map<? extends Object, ? extends Object> values = loader
                    .loadAll(keysToProcess);
            // subtract the ones that were loaded
            keysToProcess.removeAll(values.keySet());
            for (Map.Entry<? extends Object, ? extends Object> entry : values
                    .entrySet()) {
                Element newElement = new Element(entry.getKey(),
                        entry.getValue());
                underlyingCache.put(newElement);
            }
        }
        // assume we got here ok, now evict any that don't evict
        if (config.isNullRefreshEvicts() && !keysToProcess.isEmpty()) {
            underlyingCache.removeAll(keysToProcess);
        }

    }

}
