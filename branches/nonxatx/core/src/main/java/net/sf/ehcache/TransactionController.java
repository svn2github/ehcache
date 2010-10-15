package net.sf.ehcache;

import net.sf.ehcache.transaction.nonxa.TransactionContext;
import net.sf.ehcache.transaction.nonxa.TransactionException;
import net.sf.ehcache.transaction.nonxa.TransactionID;
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

    private ThreadLocal<TransactionID> currentTransactionIdThreadLocal = new ThreadLocal<TransactionID>();
    private ConcurrentMap<TransactionID, TransactionContext> contextMap = new ConcurrentHashMap<TransactionID, TransactionContext>();

    protected TransactionController() {
        //
    }

    public void begin() {
        begin(DEFAULT_TRANSACTION_TIMEOUT);
    }

    public void begin(int transactionTimeout) {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId != null) {
            throw new TransactionException("transaction already started");
        }

        TransactionContext newTx = new TransactionContext(transactionTimeout);
        contextMap.put(newTx.getTransactionId(), newTx);
        currentTransactionIdThreadLocal.set(newTx.getTransactionId());

        MDC.put(MDC_KEY, newTx.getTransactionId().toString());
        LOG.debug("begun {}", newTx.getTransactionId());
    }

    public void commit() {
        TransactionID txId = currentTransactionIdThreadLocal.get();
        if (txId == null) {
            throw new TransactionException("no transaction started");
        }

        TransactionContext currentTx = contextMap.get(txId);

        try {
            currentTx.commit();
        } finally {
            contextMap.remove(txId);
            currentTransactionIdThreadLocal.remove();
            LOG.debug("committed {}", currentTx.getTransactionId());
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
            LOG.debug("rolled back {}", currentTx.getTransactionId());
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
