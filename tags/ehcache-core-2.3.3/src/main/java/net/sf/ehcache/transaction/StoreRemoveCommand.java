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

package net.sf.ehcache.transaction;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * Represents a {@link net.sf.ehcache.store.Store#remove(Object) remove} operation to be executed on a {@link Store}.
 *
 * @author Alex Snaps
 */
public class StoreRemoveCommand implements StoreWriteCommand {

    private final CacheEntry entry;

    /**
     * Constructs a remove command for a cache entry
     *
     * @param entry to remove from the store on {@link net.sf.ehcache.transaction.StorePutCommand#execute(net.sf.ehcache.store.Store)}
     */
    public StoreRemoveCommand(final CacheEntry entry) {
        this.entry = entry;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(final Store store) {
        return store.remove(entry.getKey()) != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(final CacheWriterManager cacheWriterManager) {
        return false;
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
        return entry.getKey().equals(key);
    }

    /**
     * {@inheritDoc}
     */
    public String getCommandName() {
        return Command.REMOVE;
    }

    /**
     * Getter to the key to be removed
     *
     * @return the key
     */
    public Object getKey() {
        return entry.getKey();
    }

    /**
     * Getter to the cache entry to be removed
     *
     * @return the cache entry
     */
    public CacheEntry getEntry() {
        return entry;
    }
}
