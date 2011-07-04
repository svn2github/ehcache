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

package net.sf.ehcache.config;

/**
 * Class to hold the Pinning configuration.
 *
 * @author Ludovic Orban
 */
public class PinningConfiguration implements Cloneable {

    /**
     * Possible storage values
     */
    public static enum Storage {
        /**
         * Pin the elements on-heap
         */
        ONHEAP,

        /**
         * Pin the elements in the local VM memory
         */
        INMEMORY,

        /**
         * Pin the elements in the cache
         */
        INCACHE,
    }

    private volatile Storage storage;

    /**
     * Set the storage scope
     *
     * @param storage the storage scope
     */
    public void setStorage(String storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage must be non-null");
        }
        this.storage(Storage.valueOf(Storage.class, storage.toUpperCase()));
    }

    /**
     * Set the lowest storage from which elements must not be evicted from
     *
     * @param storage the storage, encoded as a string
     * @return this
     */
    public PinningConfiguration storage(String storage) {
        setStorage(storage);
        return this;
    }

    /**
     * Set the lowest storage from which elements must not be evicted from
     *
     * @param storage the storage
     * @return this
     */
    public PinningConfiguration storage(Storage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage must be non-null");
        }
        this.storage = storage;
        return this;
    }

    /**
     * Return the lowest storage from which elements must not be evicted from
     * @return the lowest storage from which elements must not be evicted from
     */
    public Storage getStorage() {
        return storage;
    }

}
