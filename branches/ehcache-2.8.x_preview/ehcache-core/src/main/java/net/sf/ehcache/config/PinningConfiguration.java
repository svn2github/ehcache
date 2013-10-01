/**
 *  Copyright Terracotta, Inc.
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
     * Possible store values
     */
    public static enum Store {

        /**
         * Pin the elements in the local VM memory
         */
        LOCALMEMORY,

        /**
         * Pin the elements in the cache
         */
        INCACHE,
    }

    private volatile Store store;

    /**
     * Set the store scope
     *
     * @param store the storage scope
     */
    public void setStore(String store) {
        if (store == null) {
            throw new IllegalArgumentException("Store must be non-null");
        }
        this.store(Store.valueOf(Store.class, store.toUpperCase()));
    }

    /**
     * Set the lowest store from which elements must not be evicted from
     *
     * @param store the store, encoded as a string
     * @return this
     */
    public PinningConfiguration store(String store) {
        setStore(store);
        return this;
    }

    /**
     * Set the lowest store from which elements must not be evicted from
     *
     * @param store the store
     * @return this
     */
    public PinningConfiguration store(Store store) {
        if (store == null) {
            throw new IllegalArgumentException("Store must be non-null");
        }
        this.store = store;
        return this;
    }

    /**
     * Return the lowest store from which elements must not be evicted from
     * @return the lowest store from which elements must not be evicted from
     */
    public Store getStore() {
        return store;
    }

}
