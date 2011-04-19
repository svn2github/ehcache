/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
package net.sf.ehcache.hibernate.tm;

import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * @author Alex Snaps
 */
public class SyncTransaction implements Transaction {

    private final SyncTransactionManager syncTransactionManager;
    private       Xid                    xid                    = new SyncXid();
    private       List<Synchronization>  syncs                  = new ArrayList<Synchronization>();
    private       int                    status;

    private SortedSet<EhcacheXAResource> resources = new TreeSet<EhcacheXAResource>(new Comparator<EhcacheXAResource>() {
        public int compare(final EhcacheXAResource o1, final EhcacheXAResource o2) {
            return o1.getCacheName().compareTo(o2.getCacheName());
        }
    });

    /**
     * Constructor
     * @param syncTransactionManager the initiating SyncTransactionManager
     */
    public SyncTransaction(final SyncTransactionManager syncTransactionManager) {
        this.syncTransactionManager = syncTransactionManager;
    }

    /**
     * {@inheritDoc}
     */
    public void commit()
        throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SecurityException, SystemException {
        status = Status.STATUS_COMMITTING;
        int i = 0;
        for (EhcacheXAResource resource : resources) {
            try {
                resource.commit(xid, true);
            } catch (XAException e) {
                throw new RuntimeException(e);
            }
        }
        syncTransactionManager.setTransaction(null);
        status = Status.STATUS_COMMITTED;
        afterCompletion();
    }

    /**
     * {@inheritDoc}
     */
    public boolean delistResource(final XAResource xaResource, final int i) throws IllegalStateException, SystemException {
        return resources.contains(xaResource);
    }

    /**
     * {@inheritDoc}
     */
    public boolean enlistResource(final XAResource xaResource) throws IllegalStateException, RollbackException, SystemException {
        boolean enlisted = false;
        if (xaResource instanceof EhcacheXAResource) {
            if (resources.add(((EhcacheXAResource)xaResource))) {
                enlisted = true;
                try {
                    xaResource.start(xid, 0);
                } catch (XAException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return enlisted;
    }

    /**
     * {@inheritDoc}
     */
    public int getStatus() throws SystemException {
        return status;
    }

    /**
     * {@inheritDoc}
     */
    public void registerSynchronization(final Synchronization synchronization)
        throws IllegalStateException, RollbackException, SystemException {
        syncs.add(synchronization);
    }

    /**
     * {@inheritDoc}
     */
    public void rollback() throws IllegalStateException, SystemException {
        status = Status.STATUS_ROLLING_BACK;
        for (EhcacheXAResource resource : resources) {
            try {
                resource.rollback(xid);
            } catch (XAException e) {
                throw new RuntimeException(e);
            }
        }
        syncTransactionManager.setTransaction(null);
        status = Status.STATUS_ROLLEDBACK;
        afterCompletion();
    }

    private void afterCompletion() {
        for (Synchronization sync : syncs) {
            sync.afterCompletion(status);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        status = Status.STATUS_MARKED_ROLLBACK;
    }

    /**
     * Will callback on all syncs
     */
    void beforeCompletion() {
        for (Synchronization sync : syncs) {
            sync.beforeCompletion();
        }
    }
}
