package net.sf.ehcache.config;

/**
 * Class to hold the Pinning configuration.

 * @author Ludovic Orban
 */
public class PinningConfiguration implements Cloneable {

    /**
     * Represents storage values
     */
    public static enum Storage {
        /**
         * Pin the elements on heap
         */
        ONHEAP,

        /**
         * Pin the elements in the local VM memory
         */
        INMEMORY,
    }

    private volatile Storage storage;

    public void setStorage(String storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage must be non-null");
        }
        this.storage(Storage.valueOf(Storage.class, storage.toUpperCase()));
    }

    public PinningConfiguration storage(String storage) {
        setStorage(storage);
        return this;
    }

    public PinningConfiguration storage(Storage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage must be non-null");
        }
        this.storage = storage;
        return this;
    }

    public Storage getStorage() {
        return storage;
    }
}
