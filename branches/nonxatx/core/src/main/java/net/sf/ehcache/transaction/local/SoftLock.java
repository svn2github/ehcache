package net.sf.ehcache.transaction.local;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

/**
 * A Soft locks is an element value's replacement for transactional stores
 * @author Ludovic Orban
 */
public interface SoftLock {

    /**
     * Get the key of the element this soft lock is guarding
     * @return the key
     */
    Object getKey();

    /**
     * Get the ID of the transaction under which this soft lock is operating
     * @return the TransactionID
     */
    TransactionID getTransactionID();

    /**
     * Get the element the current transaction is supposed to see.
     * @param currentTransactionId the current transaction under which this call is executed
     * @return the Element visible to the current transaction
     */
    Element getElement(TransactionID currentTransactionId);

    /**
     * Get the old Element at the key this soft lock is guarding
     * @return the Element as it was before the key was soft locked
     */
    Element getOldElement();

    /**
     * Get the new Element at the key this soft lock is guarding
     * @return the Element as it will be if the transaction under which this soft lock is operating commits
     */
    Element getNewElement();

    /**
     * Change the new Element at the key this soft lock is guarding
     * @param newElement the new Element
     */
    void setNewElement(Element newElement);

    /**
     * Lock the soft lock
     */
    void lock();

    /**
     * Attempt to lock the soft lock
     * @param ms the time in milliseconds before this method gives up
     * @return true if the soft lock was locked, false otherwise
     * @throws InterruptedException if the thread calling this method was interrupted
     */
    boolean tryLock(long ms) throws InterruptedException;

    /**
     * Unlock the soft lock. Once a soft lock got unlocked, it is considered 'dead': it cannot be
     * locked again and must be cleaned up.
     */
    void unlock();

    /**
     * Freeze the soft lock. A soft lock should only be frozen for a very short period of time as this blocks
     * the {@link #getElement(net.sf.ehcache.transaction.TransactionID)} method calls.
     * Freeze is used to mark the start of a commit / rollback phase
     */
    void freeze();

    /**
     * Unfreeze the soft lock.
     */
    void unfreeze();

    /**
     * Check if the soft lock expired, ie: that the thread which locked it is still alive
     * @return true if the soft lock is orphan and should be cleaned up, false otherwise
     */
    boolean isExpired();

    /**
     * Get the Element with which this soft lock should be replaced by a commit, rollback or clean up
     * @return the Element
     */
    Element getFrozenElement();

}
