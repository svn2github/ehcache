/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.transaction.Transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.xa.VersionAwareCommand;

/**
 * A TransactionContext represents the data local to a Transaction that involves a transactional Cache.<p>
 * It will queue operations ({@link net.sf.ehcache.transaction.Command Commands}), filter read operations on the cache (as for
 * returning null on a get on a "to be removed" key).<p>
 * It also provides access to the transaction orchestrator (like the {@link javax.transaction.TransactionManager
 * TransactionManager} in case of JTA) to the deferred operations and other contextual information. 
 * @author Alex Snaps
 */
public interface TransactionContext {

    /**
     * Add a command to the current TransactionContext
     * @param command Command to be deferred
     * @param element Element the command impacts, may be null
     */
    void addCommand(Command command, Element element);

    /**
     * Getter to the JTA Transaction this context wraps
     * @return the current Transaction
     */
    Transaction getTransaction();

    /**
     * Filter to get operations on underlying Store.<p>
     * Should the key still be transaction local, or locally pending deletion
     * @param key the key
     * @return the potential Element instance for that key
     */
    public Element get(Object key);

    /**
     * Queries the local tx context, whether the key is pending removal
     * @param key the key pending removal
     * @return true if key is pending removal
     */
    boolean isRemoved(Object key);

    /**
     * getter to all keys pending addition to the store
     * @return list of all keys
     */
    Collection getAddedKeys();

    /**
     * getter to all keys pending deletion from the store
     * @return list of all keys
     */
    Collection getRemovedKeys();

    /**
     * The underlying store's size modifier.<p>
     * Plus all pending put commands, and minus all pending removals (dependent on whether their in the underlying store)
     * @return the modifier to be applied on the {@link net.sf.ehcache.store.Store#getSize()}
     */
    int getSizeModifier();

    /**
     * All ordered pending commands
     * @return List of all pending commands
     */
    List<VersionAwareCommand> getCommands();

    /**
     * All keys to pending keys to update
     * @return UnmodifiableSet of keys pending changes
     */
    Set<Object> getUpdatedKeys();
}
