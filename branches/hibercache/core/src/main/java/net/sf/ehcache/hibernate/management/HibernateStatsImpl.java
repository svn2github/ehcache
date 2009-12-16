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

import org.hibernate.SessionFactory;

/**
 * Implementation of {@link HibernateStats}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public class HibernateStatsImpl implements HibernateStats {

    private static final double MILLIS_PER_SECOND = 1000;

    private final SessionFactory sessionFactory;

    /**
     * Constructor accepting the backing {@link SessionFactory}
     * 
     * @param sessionFactory
     */
    public HibernateStatsImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#clearStats()
     */
    public void clearStats() {
        sessionFactory.getStatistics().clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#disableStats()
     */
    public void disableStats() {
        sessionFactory.getStatistics().setStatisticsEnabled(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#enableStats()
     */
    public void enableStats() {
        sessionFactory.getStatistics().setStatisticsEnabled(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getCloseStatementCount()
     */
    public long getCloseStatementCount() {
        return sessionFactory.getStatistics().getCloseStatementCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getConnectCount()
     */
    public long getConnectCount() {
        return sessionFactory.getStatistics().getConnectCount();
    }

    /**
     * Not supported right now
     * 
     */
    public long getDBSQLExecutionSample() {
        throw new UnsupportedOperationException("Use getQueryExecutionCount() instead");
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getFlushCount()
     */
    public long getFlushCount() {
        return sessionFactory.getStatistics().getFlushCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getOptimisticFailureCount()
     */
    public long getOptimisticFailureCount() {
        return sessionFactory.getStatistics().getOptimisticFailureCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getPrepareStatementCount()
     */
    public long getPrepareStatementCount() {
        return sessionFactory.getStatistics().getPrepareStatementCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getQueryExecutionCount()
     */
    public long getQueryExecutionCount() {
        return sessionFactory.getStatistics().getQueryExecutionCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getQueryExecutionRate()
     */
    public double getQueryExecutionRate() {
        long startTime = sessionFactory.getStatistics().getStartTime();
        long now = System.currentTimeMillis();
        double deltaSecs = (now - startTime) / MILLIS_PER_SECOND;
        return getQueryExecutionCount() / deltaSecs;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getQueryExecutionSample()
     */
    public long getQueryExecutionSample() {
        throw new UnsupportedOperationException("TODO: need to impl. rates for query execution");
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getSessionCloseCount()
     */
    public long getSessionCloseCount() {
        return sessionFactory.getStatistics().getSessionCloseCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getSessionOpenCount()
     */
    public long getSessionOpenCount() {
        return sessionFactory.getStatistics().getSessionOpenCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getSuccessfulTransactionCount()
     */
    public long getSuccessfulTransactionCount() {
        return sessionFactory.getStatistics().getSuccessfulTransactionCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getTransactionCount()
     */
    public long getTransactionCount() {
        return sessionFactory.getStatistics().getTransactionCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#isStatisticsEnabled()
     */
    public boolean isStatisticsEnabled() {
        return sessionFactory.getStatistics().isStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#setStatisticsEnabled(boolean)
     */
    public void setStatisticsEnabled(boolean flag) {
        sessionFactory.getStatistics().setStatisticsEnabled(flag);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getEntityStats()
     */
    public TabularData getEntityStats() {
        throw new UnsupportedOperationException("TODO: need to impl.");
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getCollectionStats()
     */
    public TabularData getCollectionStats() {
        throw new UnsupportedOperationException("TODO: need to impl.");
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getEvictionStats()
     */
    public TabularData getEvictionStats() {
        throw new UnsupportedOperationException("TODO: need to impl.");
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.HibernateStats#getQueryStats()
     */
    public TabularData getQueryStats() {
        throw new UnsupportedOperationException("TODO: need to impl.");
    }

}
