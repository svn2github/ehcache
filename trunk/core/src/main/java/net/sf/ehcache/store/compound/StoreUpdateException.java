package net.sf.ehcache.store.compound;

import net.sf.ehcache.writer.CacheWriterManagerException;

/**
 * Exception thrown by the Store when the writer fails. Used to determine whether the element was inserted or updated in the Store
 * @author Alex Snaps
 */
public class StoreUpdateException extends CacheWriterManagerException {

    private final boolean update;

    /**
     * Constructor
     * @param e the cause of the failure
     * @param update true if element was updated, false if inserted
     */
    public StoreUpdateException(final RuntimeException e, final boolean update) {
        super(e);
        this.update = update;
    }

    /**
     * Whether the element was inserted or updated in the Store
     * @return true if element was updated, false if inserted
     */
    public boolean isUpdate() {
        return update;
    }
}
