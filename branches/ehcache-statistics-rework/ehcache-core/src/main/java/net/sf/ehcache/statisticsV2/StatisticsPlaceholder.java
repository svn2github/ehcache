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

package net.sf.ehcache.statisticsV2;

import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatisticsImpl;
import net.sf.ehcache.statisticsV2.extended.FlatExtendedStatisticsImpl;

import org.terracotta.statistics.StatisticsManager;

public class StatisticsPlaceholder extends DelegateFlatStatistics {

    public static final int DEFAULT_HISTORY_SIZE = 30;
    public static final int DEFAULT_INTERVAL_SECS = 10;
    public static final int DEFAULT_SEARCH_INTERVAL_SECS = 10;

    private final CoreStatistics core;
    private final ExtendedStatistics extended;
    private final String assocCacheName;

    public StatisticsPlaceholder(Ehcache ehcache, StatisticsManager statisticsManager) {
        super();
        this.assocCacheName=ehcache.getName();
        extended = new ExtendedStatisticsImpl(statisticsManager, 5, TimeUnit.MINUTES);
        core=new CoreStatisticsImpl(extended);
        init(new FlatCoreStatisticsImpl(core), new FlatExtendedStatisticsImpl(extended));
    }

    public CoreStatistics getCore() {
        return core;
    }

    public ExtendedStatistics getExtended() {
        return extended;
    }

    public void setStatisticsEnabled(boolean b) {
        throw new UnsupportedOperationException();
    }

    public void setSampledStatisticsEnabled(boolean b) {
        throw new UnsupportedOperationException();
    }

    public String getAssociatedCacheName() {
        return assocCacheName;
    }

    public void clearStatistics() {
        throw new UnsupportedOperationException();
    }

    public boolean isStatisticsEnabled() {
        throw new UnsupportedOperationException();
    }

    public int getStatisticsAccuracy() {
        throw new UnsupportedOperationException();
    }

    public String getStatisticsAccuracyDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isSampledStatisticsEnabled() {
        // TODO Auto-generated method stub
        return false;
    }



}
