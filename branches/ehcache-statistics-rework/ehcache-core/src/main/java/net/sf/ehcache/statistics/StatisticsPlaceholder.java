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

package net.sf.ehcache.statistics;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import net.sf.ehcache.statistics.extended.ExtendedStatisticsImpl;
import net.sf.ehcache.statistics.extended.FlatExtendedStatisticsImpl;

import org.terracotta.statistics.StatisticsManager;

/**
 * StatisticsPlaceholder class.
 *
 * @author cschanck
 */
public class StatisticsPlaceholder extends DelegateFlatStatistics {

    /** The Constant DEFAULT_HISTORY_SIZE. */
    public static final int DEFAULT_HISTORY_SIZE = 30;

    /** The Constant DEFAULT_INTERVAL_SECS. */
    public static final int DEFAULT_INTERVAL_SECS = 10;

    /** The Constant DEFAULT_SEARCH_INTERVAL_SECS. */
    public static final int DEFAULT_SEARCH_INTERVAL_SECS = 10;

    private static final int DEFAULT_TIME_TO_DISABLE_MINS = 5;

    /** The core. */
    private final CoreStatistics core;

    /** The extended statistics. */
    private final ExtendedStatisticsImpl extended;

    /** The associated cache name. */
    private final String assocCacheName;

    /**
     * Instantiates a new statistics placeholder.
     *
     * @param ehcache the ehcache
     * @param statisticsManager the statistics manager
     */
    public StatisticsPlaceholder(Ehcache ehcache, ScheduledExecutorService executor) {
        StatisticsManager statsManager = new StatisticsManager();
        statsManager.root(ehcache);
        this.assocCacheName = ehcache.getName();
        this.extended = new ExtendedStatisticsImpl(statsManager, executor, DEFAULT_TIME_TO_DISABLE_MINS, TimeUnit.MINUTES);
        this.core = new CoreStatisticsImpl(extended);
        init(new FlatCoreStatisticsImpl(core), new FlatExtendedStatisticsImpl(extended));
    }

    /**
     * Gets the core.
     *
     * @return the core
     */
    public CoreStatistics getCore() {
        return core;
    }

    /**
     * Gets the extended.
     *
     * @return the extended
     */
    public ExtendedStatistics getExtended() {
        return extended;
    }

    /**
     * Sets the statistics enabled.
     *
     * @param b the new statistics enabled
     */
    public void setStatisticsEnabled(boolean b) {
        //no-op
    }

    /**
     * Sets the sampled statistics enabled.
     *
     * @param b the new sampled statistics enabled
     */
    public void setSampledStatisticsEnabled(boolean b) {
        //no-op
    }

    /**
     * Gets the associated cache name.
     *
     * @return the associated cache name
     */
    public String getAssociatedCacheName() {
        return assocCacheName;
    }

    /**
     * Clear statistics.
     */
    public void clearStatistics() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if is statistics enabled.
     *
     * @return true, if is statistics enabled
     */
    public boolean isStatisticsEnabled() {
        return true;
    }

    /**
     * Gets the statistics accuracy.
     *
     * @return the statistics accuracy
     */
    public int getStatisticsAccuracy() {
        return 0;
    }

    /**
     * Gets the statistics accuracy description.
     *
     * @return the statistics accuracy description
     */
    public String getStatisticsAccuracyDescription() {
        return "default";
    }

    /**
     * Checks if is sampled statistics enabled.
     *
     * @return true, if is sampled statistics enabled
     */
    public boolean isSampledStatisticsEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

}
