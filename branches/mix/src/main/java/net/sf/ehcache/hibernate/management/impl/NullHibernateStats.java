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

package net.sf.ehcache.hibernate.management.impl;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.openmbean.TabularData;

import net.sf.ehcache.hibernate.management.api.HibernateStats;

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
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#clearStats()
     */
    public void clearStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#disableStats()
     */
    public void disableStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#enableStats()
     */
    public void enableStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getCloseStatementCount()
     */
    public long getCloseStatementCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getCollectionStats()
     */
    public TabularData getCollectionStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getConnectCount()
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
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getEntityStats()
     */
    public TabularData getEntityStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getFlushCount()
     */
    public long getFlushCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getOptimisticFailureCount()
     */
    public long getOptimisticFailureCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getPrepareStatementCount()
     */
    public long getPrepareStatementCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getQueryExecutionCount()
     */
    public long getQueryExecutionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getQueryExecutionRate()
     */
    public double getQueryExecutionRate() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getQueryExecutionSample()
     */
    public long getQueryExecutionSample() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getQueryStats()
     */
    public TabularData getQueryStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getSessionCloseCount()
     */
    public long getSessionCloseCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getSessionOpenCount()
     */
    public long getSessionOpenCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getSuccessfulTransactionCount()
     */
    public long getSuccessfulTransactionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getTransactionCount()
     */
    public long getTransactionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#isStatisticsEnabled()
     */
    public boolean isStatisticsEnabled() {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#setStatisticsEnabled(boolean)
     */
    public void setStatisticsEnabled(boolean flag) {
        // no-op
    }

    /**
     * @see net.sf.ehcache.hibernate.management.api.HibernateStats#getCacheRegionStats()
     */
    public TabularData getCacheRegionStats() {
        return null;
    }

    /**
     * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
     */
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        /**/
    }

    /**
     * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
     */
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
        /**/
    }

    /**
     * @see javax.management.NotificationBroadcaster#getNotificationInfo()
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        return null;
    }

    /**
     * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
     */
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        /**/
    }
}
