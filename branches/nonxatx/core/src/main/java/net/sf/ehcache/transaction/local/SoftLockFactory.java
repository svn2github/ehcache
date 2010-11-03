package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import java.util.Set;

/**
 * A factory for {@link SoftLock}s
 * @author Ludovic Orban
 */
public interface SoftLockFactory {

    /**
     * Create a new, unlocked soft lock
     * @param transactionID the transaction ID under which the soft lock will operate
     * @param key the key of the Element this soft lock is protecting
     * @param newElement the new Element
     * @param oldElement the actual Element
     * @return the soft lock
     */
    SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement);

    /**
     * Get a Set of keys protected by soft locks which must not be visible to a transaction context
     * according to the isolation level.
     * @param transactionContext the transaction context
     * @return a Set of keys invisible to the context
     */
    Set<Object> getKeysInvisibleInContext(TransactionContext transactionContext);

}
