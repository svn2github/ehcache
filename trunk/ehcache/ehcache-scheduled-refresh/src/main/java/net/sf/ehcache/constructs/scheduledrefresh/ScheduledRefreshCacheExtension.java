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

import java.util.Properties;
import java.util.logging.Logger;

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
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.terracotta.quartz.TerracottaJobStore;

/**
 * This is class provides a cache extension which allows for the scheduled
 * refresh of all keys currently in the cache, using whichever CacheLoader's are
 * defined. It uses Quartz to do the job scheduling.
 * 
 * @author cschanck
 * 
 */
public class ScheduledRefreshCacheExtension implements CacheExtension {

    /**
     * Logger this package uses.
     */
    static final Logger LOG = Logger
            .getLogger(ScheduledRefreshCacheExtension.class.getName());

    /**
     * Job Property key for Terracotta Job Store to use.
     */
    static final String PROP_QUARTZ_JOB_STORE_TC_CONFIG_URL = "org.quartz.jobStore.tcConfigUrl";

    /**
     * Job Property key for cache name this extension's jobs will run against
     */
    static final String PROP_CACHE_NAME = ScheduledRefreshCacheExtension.class
            .getName() + ".cacheName";

    /**
     * Job Property key for cache manager name this extension's jobs will run
     * against
     */
    static final String PROP_CACHE_MGR_NAME = ScheduledRefreshCacheExtension.class
            .getName() + "cacheManagerName";

    /**
     * Job Property key for sending config object to scheduled job
     */
    static final String PROP_CONFIG_OBJECT = ScheduledRefreshCacheExtension.class
            .getName() + "scheduledRefreshConfig";

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

    private boolean isLocalJobStore;

    /**
     * Constructor. Create an extension with the specified config object against the specified cache.
     * @param config Configuration to use.
     * @param cache Cache to process against.
     */
    public ScheduledRefreshCacheExtension(ScheduledRefreshConfiguration config,
            Ehcache cache) {
        this.underlyingCache = cache;
        this.config = config;
        this.status = Status.STATUS_UNINITIALISED;
    }

    @Override
    public void init() {
        if (config.getUniqueNamePart() != null) {
            this.name = "scheduledRefresh_" + underlyingCache.getCacheManager().getName()
                    + "_" + underlyingCache.getName() + "_" + config.getUniqueNamePart();
        } else {
            this.name = "scheduledRefresh_" + underlyingCache.getCacheManager().getName()
                    + "_" + underlyingCache.getName();
        }
        this.groupName = name + "_grp";
        try {
            makeAndStartQuartzScheduler();
            JobDataMap jdm = new JobDataMap();
            jdm.put(PROP_CACHE_MGR_NAME, underlyingCache.getCacheManager()
                    .getName());
            jdm.put(PROP_CACHE_NAME, underlyingCache.getName());
            jdm.put(PROP_CONFIG_OBJECT, config);
            JobDetail seed = JobBuilder.newJob(OverseerJob.class)
                    .withIdentity("seed", groupName).usingJobData(jdm).build();
            scheduler.addJob(seed, true);
            CronTrigger trigger = TriggerBuilder
                    .newTrigger()
                    .forJob(seed)
                    .withSchedule(
                            CronScheduleBuilder.cronSchedule(config
                                    .getCronExpression())).build();
            scheduler.scheduleJob(trigger);
            status = Status.STATUS_ALIVE;
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    private void makeAndStartQuartzScheduler() throws SchedulerException {
        Properties props = new Properties();
        props.put(StdSchedulerFactory.PROP_SCHED_NAME, name);
        props.put(StdSchedulerFactory.PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON,
                Boolean.TRUE.toString());
        props.put("org.quartz.threadPool.threadCount",
                Integer.toString(config.getQuartzThreadCount()));
        if (config.getTerracottaConfigUrl() != null) {
            this.isLocalJobStore=false;
            props.put(PROP_QUARTZ_JOB_STORE_TC_CONFIG_URL,
                    config.getTerracottaConfigUrl());
            props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS,
                    TerracottaJobStore.class.getName());
        } else {
            this.isLocalJobStore=true;
            props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS,
                    RAMJobStore.class.getName());
        }

        StdSchedulerFactory factory = new StdSchedulerFactory(props);
        scheduler = factory.getScheduler(name);
        if (scheduler == null) {
            scheduler = factory.getScheduler();
        }
        scheduler.start();
    }

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
    public CacheExtension clone(Ehcache cache)
            throws CloneNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status getStatus() {
        return status;
    }

}
