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
package net.sf.ehcache.transaction.xa;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.xa.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An XATransactionContext represents the data local to a Transaction that involves a transactional Cache.<p>
 * It will queue operations ({@link Command Commands}), filter read operations on the cache (as for
 * returning null on a get on a "to be removed" key).<p>
 *
 * @author Ludovic Orban
 */
public class XATransactionContext {

    private static final Logger LOG = LoggerFactory.getLogger(XATransactionContext.class.getName());

    private final ConcurrentMap<Object, Element> commandElements = new ConcurrentHashMap<Object, Element>();
    private final Set<Object> removedKeys = new HashSet<Object>();
    private final Set<Object> addedKeys = new HashSet<Object>();
    private int sizeModifier;


    private final Map<Object, Command> commands = new LinkedHashMap<Object, Command>();
    private final Store underlyingStore;

    /**
     * Constructor
     *
     * @param underlyingStore the underlying store
     */
    public XATransactionContext(Store underlyingStore) {
        this.underlyingStore = underlyingStore;
    }

    /**
     * Add a command to the current LocalTransactionContext
     *
     * @param command Command to be deferred
     * @param element Element the command impacts, may be null
     */
    public void addCommand(final Command command, final Element element) {
        Object key = command.getObjectKey();

        if (element != null) {
            commandElements.put(key, element);
        } else {
            commandElements.remove(key);
        }

        if (command.isPut(key)) {
            boolean removed = removedKeys.remove(key);
            boolean added = addedKeys.add(key);
            if (removed || added && !underlyingStore.containsKey(key)) {
                sizeModifier++;
            }
        } else if (command.isRemove(key)) {
            removedKeys.add(key);
            if (addedKeys.remove(key) || underlyingStore.containsKey(key)) {
                sizeModifier--;
            }
        }

        commands.put(key, command);

        LOG.debug("XA context added new command [{}], it now contains {} command(s)", command, commands.size());
    }

    /**
     * All ordered pending commands
     *
     * @return List of all pending commands
     */
    public List<Command> getCommands() {
        return new ArrayList<Command>(commands.values());
    }

    /**
     * Filter to get operations on underlying Store.<p>
     * Should the key still be transaction local, or locally pending deletion
     *
     * @param key the key
     * @return the potential Element instance for that key
     */
    public Element get(Object key) {
        return removedKeys.contains(key) ? null : commandElements.get(key);
    }

    /**
     * Queries the local tx context, whether the key is pending removal
     *
     * @param key the key pending removal
     * @return true if key is pending removal
     */
    public boolean isRemoved(Object key) {
        return removedKeys.contains(key);
    }

    /**
     * Queries the local tx context, whether the key is pending removal
     *
     * @return true if key is pending removal
     */
    public Collection getAddedKeys() {
        return Collections.unmodifiableSet(addedKeys);
    }

    /**
     * getter to all keys pending deletion from the store
     *
     * @return list of all keys
     */
    public Collection getRemovedKeys() {
        return Collections.unmodifiableSet(removedKeys);
    }

    /**
     * The underlying store's size modifier.<p>
     * Plus all pending put commands, and minus all pending removals (dependent on whether their in the underlying store)
     *
     * @return the modifier to be applied on the {@link net.sf.ehcache.store.Store#getSize()}
     */
    public int getSizeModifier() {
        return sizeModifier;
    }

    @Override
    public String toString() {
        return "XATransactionContext with " + commands.size() + " command(s)";
    }
}
