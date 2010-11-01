package net.sf.ehcache.transaction.local;

import net.sf.ehcache.transaction.TransactionID;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lorban
 */
public final class TransactionIDImpl implements TransactionID {

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private final int id;
    private volatile boolean commit;

    TransactionIDImpl() {
        this.id = idGenerator.getAndIncrement();
    }

    public boolean isDecisionCommit() {
        return commit;
    }

    public void markAsCommit(boolean commit) {
        this.commit = commit;
    }


    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof TransactionIDImpl) {
            TransactionIDImpl otherId = (TransactionIDImpl) obj;
            return id == otherId.id;
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "" + id;
    }
}
