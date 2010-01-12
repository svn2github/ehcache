/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.hibernate.management;

import javax.management.openmbean.TabularData;

/**
 * Implementation of {@link HibernateStats} that does nothing
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public final class NullHibernateStats implements HibernateStats {

    /**
     * Singleton instance.
     */
    public static final HibernateStats INSTANCE = new NullHibernateStats();

    /**
     * private constructor. No need to create instances of this. Use singleton instance
     */
    private NullHibernateStats() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#clearStats()
     */
    public void clearStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#disableStats()
     */
    public void disableStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#enableStats()
     */
    public void enableStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getCloseStatementCount()
     */
    public long getCloseStatementCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getCollectionStats()
     */
    public TabularData getCollectionStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getConnectCount()
     */
    public long getConnectCount() {
        // no-op
        return 0;
    }

    /**
     * Not supported right now
     */
    public long getDBSQLExecutionSample() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getEntityStats()
     */
    public TabularData getEntityStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getEvictionStats()
     */
    public TabularData getEvictionStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getFlushCount()
     */
    public long getFlushCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getOptimisticFailureCount()
     */
    public long getOptimisticFailureCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getPrepareStatementCount()
     */
    public long getPrepareStatementCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getQueryExecutionCount()
     */
    public long getQueryExecutionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getQueryExecutionRate()
     */
    public double getQueryExecutionRate() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getQueryExecutionSample()
     */
    public long getQueryExecutionSample() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getQueryStats()
     */
    public TabularData getQueryStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getSessionCloseCount()
     */
    public long getSessionCloseCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getSessionOpenCount()
     */
    public long getSessionOpenCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getSuccessfulTransactionCount()
     */
    public long getSuccessfulTransactionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getTransactionCount()
     */
    public long getTransactionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#isStatisticsEnabled()
     */
    public boolean isStatisticsEnabled() {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#setStatisticsEnabled(boolean)
     */
    public void setStatisticsEnabled(boolean flag) {
        // no-op

    }

}
