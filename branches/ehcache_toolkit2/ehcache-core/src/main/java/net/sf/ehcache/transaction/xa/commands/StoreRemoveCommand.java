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

package net.sf.ehcache.transaction.xa.commands;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Element;

/**
 * Represents a {@link net.sf.ehcache.store.Store#remove(Object) remove} operation to be executed on a {@link net.sf.ehcache.store.Store}.
 *
 * @author Alex Snaps
 */
public class StoreRemoveCommand extends AbstractStoreCommand {

    private Object key;

    /**
     * Create a StoreRemoveCommand
     *
     * @param key the key of the element to remove
     * @param oldElement the element in the underlying store at the time this command is created
     */
    public StoreRemoveCommand(final Object key, final Element oldElement) {
        super(oldElement, null);
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPut(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRemove(Object key) {
        return getObjectKey().equals(key);
    }


    /**
     * {@inheritDoc}
     */
    public Object getObjectKey() {
        return key;
    }

    /**
     * Getter to the cache entry to be removed
     *
     * @return the cache entry
     */
    public CacheEntry getEntry() {
        return new CacheEntry(key, getOldElement());
    }
}
