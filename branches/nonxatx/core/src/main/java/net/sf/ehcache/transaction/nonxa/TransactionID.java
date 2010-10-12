package net.sf.ehcache.transaction.nonxa;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lorban
 */
public final class TransactionID implements Serializable {

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private final int id;

    public TransactionID() {
        this.id = idGenerator.getAndIncrement();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof TransactionID) {
            TransactionID otherId = (TransactionID) obj;
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
        return "TransactionID [" + id + "]";
    }
}
