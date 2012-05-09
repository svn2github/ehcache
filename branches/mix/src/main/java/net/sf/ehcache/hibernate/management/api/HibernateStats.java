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

package net.sf.ehcache.hibernate.management.api;

import javax.management.NotificationEmitter;
import javax.management.openmbean.TabularData;

/**
 * Interface for hibernate related statistics of hibernate second level cache
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public interface HibernateStats extends NotificationEmitter {
    /**
     * CACHE_ENABLED
     */
    public static final String CACHE_ENABLED = "CacheEnabled";
    
    /**
     * CACHE_REGION_CHANGED
     */
    public static final String CACHE_REGION_CHANGED = "CacheRegionChanged";
    
    /**
     * CACHE_FLUSHED
     */
    public static final String CACHE_FLUSHED = "CacheFlushed";
    
    /**
     * CACHE_REGION_FLUSHED
     */
    public static final String CACHE_REGION_FLUSHED = "CacheRegionFlushed";
    
    /**
     * CACHE_STATISTICS_ENABLED
     */
    public static final String CACHE_STATISTICS_ENABLED = "CacheStatisticsEnabled";
    
    /**
     * CACHE_STATISTICS_RESET
     */
    public static final String CACHE_STATISTICS_RESET = "CacheStatisticsReset";

    /**
     * Returns true if statistics collection is enabled otherwise false
     * 
     * @return Returns true if statistics collection is enabled otherwise false
     */
    boolean isStatisticsEnabled();

    /**
     * Enable/Disable statistics collection
     * 
     * @param flag
     */
    void setStatisticsEnabled(boolean flag);

    /**
     * Enables statistics collection
     */
    void enableStats();

    /**
     * Disables statistics collection
     */
    void disableStats();

    /**
     * Clears statistics, resets all counters to zero
     */
    void clearStats();

    /**
     * Returns the query execution count. This includes only HQL's
     * 
     * @return Returns the query execution count. This includes only HQL's
     */
    long getQueryExecutionCount();

    // /**
    // * Returns last count sample of <b>all SQL's</b> getting executed in the DB.
    // */
    // long getDBSQLExecutionSample();

    /**
     * Returns last count sample of <b>HQL's</b> getting executed in the DB.
     * 
     * @return Returns last count sample of <b>HQL's</b> getting executed in the DB.
     */
    long getQueryExecutionSample();

    /**
     * Returns rate of HQL query executed in the DB
     * 
     * @return Returns rate of HQL query executed in the DB
     */
    double getQueryExecutionRate();

    /**
     * Returns the count of close statements
     * 
     * @return Returns the count of close statementss
     */
    long getCloseStatementCount();

    /**
     * Return connect counts
     * 
     * @return Return connect counts
     */
    long getConnectCount();

    /**
     * Returns flush count
     * 
     * @return Returns flush count
     */
    long getFlushCount();

    /**
     * Returns Optimistic failure count
     * 
     * @return Returns Optimistic failure count
     */
    long getOptimisticFailureCount();

    /**
     * Returns prepare statement count
     * 
     * @return Returns prepare statement count
     */
    long getPrepareStatementCount();

    /**
     * Returns session close count
     * 
     * @return Returns session close count
     */
    long getSessionCloseCount();

    /**
     * Returns session open count
     * 
     * @return Returns session open count
     */
    long getSessionOpenCount();

    /**
     * Returns successful transaction count
     * 
     * @return Returns successful transaction count
     */
    long getSuccessfulTransactionCount();

    /**
     * Returns transaction count
     * 
     * @return Returns transaction count
     */
    long getTransactionCount();

    /**
     * Returns {@link TabularData} of entity stats
     * 
     * @return Returns {@link TabularData} of entity stats
     */
    TabularData getEntityStats();

    /**
     * Returns {@link TabularData} of collection stats
     * 
     * @return Returns {@link TabularData} of collection stats
     */
    TabularData getCollectionStats();

    /**
     * Returns {@link TabularData} of query stats
     * 
     * @return Returns {@link TabularData} of query stats
     */
    TabularData getQueryStats();

    /**
     * Returns {@link TabularData} of cache region stats
     * 
     * @return Returns {@link TabularData} of cache region stats
     */
    TabularData getCacheRegionStats();    
}
