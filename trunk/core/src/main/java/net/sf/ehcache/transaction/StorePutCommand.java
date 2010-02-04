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

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;

/**
 * Represents a {@link net.sf.ehcache.store.Store#put(net.sf.ehcache.Element)} put} operation to be executed on a {@link Store}.
 * @author Alex Snaps
 */
public class StorePutCommand implements StoreWriteCommand {

    private final Element element;

    /**
     * Constructs a put command for an Element
     * @param element to put in the store on {@link net.sf.ehcache.transaction.StorePutCommand#execute(net.sf.ehcache.store.Store)}
     */
    public StorePutCommand(final Element element) {
        this.element = element;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(final Store store) {
        store.put(element);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPut(Object key) {
        return element.getKey().equals(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRemove(Object key) {
        return false;
    }

    /**
     * Getter to the Element instance to be put in the Store
     * @return the element instance
     */
    public Element getElement() {
        return element;
    }

    /**
     * {@inheritDoc}
     */
    public String getCommandName() {
        return Command.PUT;
    }
}
