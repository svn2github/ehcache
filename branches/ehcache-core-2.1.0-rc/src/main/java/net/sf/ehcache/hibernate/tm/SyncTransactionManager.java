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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * @author Alex Snaps
 */
public final class SyncTransactionManager implements TransactionManager {

    private static final ThreadLocal<Transaction> CURRENT_TX = new ThreadLocal<Transaction>();

    private final TransactionManager transactionManager;

    /**
     * Constructor
     * @param transactionManager the TransactionManager to decorate
     */
    public SyncTransactionManager(final TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * {@inheritDoc}
     */
    public void begin() throws NotSupportedException, SystemException {
        requiresTx(false);
        Transaction underlyingTx = transactionManager.getTransaction();
        if (underlyingTx == null) {
            throw new IllegalStateException();
        }
        SyncTransaction transaction = new SyncTransaction(this);
        try {
            underlyingTx.registerSynchronization(new TransactionSynchronization(transaction));
        } catch (RollbackException e) {
            throw new RuntimeException(e);
        }
        setTransaction(transaction);
    }

    /**
     * {@inheritDoc}
     */
    public void commit() throws HeuristicMixedException, HeuristicRollbackException,
        IllegalStateException, RollbackException, SecurityException, SystemException {
        transactionManager.commit();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatus() throws SystemException {
        Transaction tx = getTransaction();
        return tx != null ? tx.getStatus() : Status.STATUS_NO_TRANSACTION;
    }

    /**
     * {@inheritDoc}
     */
    public Transaction getTransaction() throws SystemException {
        Transaction transaction = CURRENT_TX.get();
        if (transaction == null) {
            try {
                this.begin();
                transaction = getTransaction();
            } catch (NotSupportedException e) {
                throw new IllegalStateException(e);
            }
        }
        return transaction;
    }

    /**
     * sets the current Transaction for the Thread
     * @param tx the current transaction, null if none
     */
    public void setTransaction(Transaction tx) {
        CURRENT_TX.set(tx);
    }

    /**
     * {@inheritDoc}
     */
    public void resume(final Transaction transaction) throws IllegalStateException, InvalidTransactionException, SystemException {
        requiresTx(false);
        setTransaction(transaction);
    }

    private void requiresTx(boolean required) throws SystemException {
        if (isTxInFlight() != required) {
            throw new IllegalStateException();
        }
    }

    private boolean isTxInFlight() {
        return CURRENT_TX.get() != null;
    }

    /**
     * {@inheritDoc}
     */
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        transactionManager.rollback();
    }

    /**
     * {@inheritDoc}
     */
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        requiresTx(true);
        getTransaction().setRollbackOnly();
    }

    /**
     * {@inheritDoc}
     */
    public void setTransactionTimeout(final int i) throws SystemException {
    }

    /**
     * {@inheritDoc}
     */
    public Transaction suspend() throws SystemException {
        Transaction transaction = getTransaction();
        setTransaction(null);
        return transaction;
    }
}
