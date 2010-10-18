package net.sf.ehcache.transaction.nonxa;

import net.sf.ehcache.TransactionController;

import javax.transaction.*;

/**
 * @author Ludovic Orban
 */
public class NonXaEhcacheSynchronization implements Synchronization {

    private TransactionController transactionController;

    public NonXaEhcacheSynchronization(TransactionController transactionController) {
        this.transactionController = transactionController;
    }

    public void beforeCompletion() {
        //
    }

    public void afterCompletion(int status) {
        if (status == javax.transaction.Status.STATUS_COMMITTED) {
            transactionController.commit();
        } else {
            transactionController.rollback();
        }
    }

    @Override
    public String toString() {
        return "NonXaEhcacheSynchronization of transaction [" + transactionController.getCurrentTransactionContext().getTransactionId() + "]";
    }
}
