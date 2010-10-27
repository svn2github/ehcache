package net.sf.ehcache;

import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.local.TransactionContext;
import net.sf.ehcache.transaction.local.TransactionException;
import net.sf.ehcache.transaction.local.TransactionIDFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author lorban
 */
public final class TransactionController {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionController.class.getName());

    private static final String MDC_KEY = "__ehcache_txId";
    private static final int DEFAULT_TRANSACTION_TIMEOUT = 15;

    private final ThreadLocal<TransactionID> currentTransactionIdThreadLocal = new ThreadLocal<TransactionID>();
    private final ConcurrentMap<TransactionID, TransactionContext> contextMap = new ConcurrentHashMap<TransactionID, TransactionContext>();
    private final TransactionIDFactory transactionIDFactory;

    protected TransactionController(TransactionIDFactory transactionIDFactory) {
        this.transactionIDFactory = transactionIDFactory;
    }

    public void begin() {
        begin(DEFAULT_TRANSACTION_TIMEOUT);
    }

    public void begin(int transactionTimeout) {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId != null) {
            throw new TransactionException("transaction already started");
        }

        TransactionContext newTx = new TransactionContext(transactionTimeout, transactionIDFactory.createTransactionID());
        contextMap.put(newTx.getTransactionId(), newTx);
        currentTransactionIdThreadLocal.set(newTx.getTransactionId());

        MDC.put(MDC_KEY, newTx.getTransactionId().toString());
        LOG.debug("begun transaction {}", newTx.getTransactionId());
    }

    public void commit() {
        commit(false);
    }

    public void commit(boolean ignoreTimeout) {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            throw new TransactionException("no transaction started");
        }

        TransactionContext currentTx = contextMap.get(txId);

        try {
            currentTx.commit(ignoreTimeout);
        } finally {
            contextMap.remove(txId);
            currentTransactionIdThreadLocal.remove();
            LOG.debug("committed transaction {}", currentTx.getTransactionId());
            MDC.remove(MDC_KEY);
        }
    }

    public void rollback() {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            throw new TransactionException("no transaction started");
        }

        TransactionContext currentTx = contextMap.get(txId);

        try {
            currentTx.rollback();
        } finally {
            contextMap.remove(txId);
            currentTransactionIdThreadLocal.remove();
            LOG.debug("rolled back transaction {}", currentTx.getTransactionId());
            MDC.remove(MDC_KEY);
        }
    }

    public void setRollbackOnly() {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            throw new TransactionException("no transaction started");
        }

        TransactionContext currentTx = contextMap.get(txId);

        currentTx.setRollbackOnly(true);
    }

    public TransactionContext getCurrentTransactionContext() {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            return null;
        }
        return contextMap.get(txId);
    }

}
