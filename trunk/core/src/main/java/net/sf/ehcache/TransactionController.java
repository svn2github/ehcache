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
package net.sf.ehcache;

import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.local.LocalTransactionContext;
import net.sf.ehcache.transaction.TransactionException;
import net.sf.ehcache.transaction.TransactionID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TransactionController is used to begin, commit and rollback local transactions
 *
 * @author Ludovic Orban
 */
public final class TransactionController {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionController.class.getName());
    private static final String MDC_KEY = "ehcache-txid";
    private static final int DEFAULT_TRANSACTION_TIMEOUT = 15;

    private final ThreadLocal<TransactionID> currentTransactionIdThreadLocal = new ThreadLocal<TransactionID>();
    private final ConcurrentMap<TransactionID, LocalTransactionContext> contextMap =
            new ConcurrentHashMap<TransactionID, LocalTransactionContext>();
    private final TransactionIDFactory transactionIDFactory;

    private volatile int defaultTransactionTimeout = DEFAULT_TRANSACTION_TIMEOUT;

    /**
     * Create a TransactionController instance
     * @param transactionIDFactory the TransactionIDFactory
     */
    TransactionController(TransactionIDFactory transactionIDFactory) {
        this.transactionIDFactory = transactionIDFactory;
    }

    /**
     * Get the default transaction timeout in seconds
     * @return the default transaction timeout
     */
    public int getDefaultTransactionTimeout() {
        return defaultTransactionTimeout;
    }

    /**
     * Set the default transaction timeout in seconds, it must be > 0
     * @param defaultTransactionTimeoutSeconds the default transaction timeout
     */
    public void setDefaultTransactionTimeout(int defaultTransactionTimeoutSeconds) {
        if (defaultTransactionTimeoutSeconds < 0) {
            throw new IllegalArgumentException("timeout cannot be < 0");
        }
        this.defaultTransactionTimeout = defaultTransactionTimeoutSeconds;
    }

    /**
     * Begin a new transaction and bind its context to the current thread
     */
    public void begin() {
        begin(defaultTransactionTimeout);
    }

    /**
     * Begin a new transaction with the specified timeout and bind its context to the current thread
     * @param transactionTimeoutSeconds the timeout foe this transaction in seconds
     */
    public void begin(int transactionTimeoutSeconds) {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId != null) {
            throw new TransactionException("transaction already started");
        }

        LocalTransactionContext newTx = new LocalTransactionContext(transactionTimeoutSeconds, transactionIDFactory.createTransactionID());
        contextMap.put(newTx.getTransactionId(), newTx);
        currentTransactionIdThreadLocal.set(newTx.getTransactionId());

        MDC.put(MDC_KEY, newTx.getTransactionId().toString());
        LOG.debug("begun transaction {}", newTx.getTransactionId());
    }

    /**
     * Commit the transaction bound to the current thread
     */
    public void commit() {
        commit(false);
    }

    /**
     * Commit the transaction bound to the current thread, ignoring if the transaction
     * timed out
     * @param ignoreTimeout true if the transaction should be committed no matter if it timed out or not
     */
    public void commit(boolean ignoreTimeout) {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            throw new TransactionException("no transaction started");
        }

        LocalTransactionContext currentTx = contextMap.get(txId);

        try {
            currentTx.commit(ignoreTimeout);
        } finally {
            contextMap.remove(txId);
            currentTransactionIdThreadLocal.remove();
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Rollback the transaction bound to the current thread
     */
    public void rollback() {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            throw new TransactionException("no transaction started");
        }

        LocalTransactionContext currentTx = contextMap.get(txId);

        try {
            currentTx.rollback();
        } finally {
            contextMap.remove(txId);
            currentTransactionIdThreadLocal.remove();
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Mark the transaction bound to the current thread for rollback only
     */
    public void setRollbackOnly() {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            throw new TransactionException("no transaction started");
        }

        LocalTransactionContext currentTx = contextMap.get(txId);

        currentTx.setRollbackOnly();
    }

    /**
     * Get the transaction context bond to the current thread
     * @return the transaction context bond to the current thread or null if no transaction
     *      started on the current thread          
     */
    public LocalTransactionContext getCurrentTransactionContext() {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            return null;
        }
        return contextMap.get(txId);
    }

}
