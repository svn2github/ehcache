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

public class StatisticsPlaceholder {

    private final CoreStatistics core=new CoreStatisticsPlaceholder();

    private final ExtendedStatistics extended=new ExtendedStatisticsPlaceholder();

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



}
