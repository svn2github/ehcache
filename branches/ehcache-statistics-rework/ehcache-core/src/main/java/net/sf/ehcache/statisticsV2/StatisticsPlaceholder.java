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

public class StatisticsPlaceholder {

    public static final int DEFAULT_HISTORY_SIZE = 30;
    public static final int DEFAULT_INTERVAL_SECS = 10;
    public static final int DEFAULT_SEARCH_INTERVAL_SECS = 10;

    private final CoreStatistics core;
    private final FlatCoreStatistics flatCore;
    private final ExtendedStatistics extended;
    private final FlatExtendedStatisticsImpl flatExtended;

    public StatisticsPlaceholder(Ehcache ehcache, StatisticsManager statisticsManager) {
        extended = new ExtendedStatisticsImpl(statisticsManager, 5, TimeUnit.MINUTES);
        flatExtended=new FlatExtendedStatisticsImpl(extended);
        core=new CoreStatisticsImpl(extended);
        flatCore=new FlatCoreStatisticsImpl(core);
    }

    public CoreStatistics getCore() {
        return core;
    }

    public FlatCoreStatistics getFlatCore() {
        return flatCore;
    }

    public ExtendedStatistics getExtended() {
        return extended;
    }

    public FlatExtendedStatisticsImpl getFlatExtended() {
        return flatExtended;
    }

    public void setStatisticsEnabled(boolean b) {
        throw new UnsupportedOperationException();
    }

    public void setSampledStatisticsEnabled(boolean b) {
        throw new UnsupportedOperationException();
    }

    public String getAssociatedCacheName() {
        throw new UnsupportedOperationException();
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
