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

import net.sf.ehcache.Element;

/**
 * Represents a {@link net.sf.ehcache.store.Store#put(net.sf.ehcache.Element)} put} operation to be executed on a {@link net.sf.ehcache.store.Store}.
 * @author Alex Snaps
 */
public class StorePutCommand extends AbstractStoreCommand {

    /**
     * Create a StorePutCommand
     *
     * @param oldElement the element in the underlying store at the time this command is created
     * @param newElement the new element to put in the underlying store
     */
    public StorePutCommand(final Element oldElement, final Element newElement) {
        super(oldElement, newElement);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPut(Object key) {
        return getObjectKey().equals(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRemove(Object key) {
        return false;
    }


    /**
     * Getter to the Element instance to be put in the Store
     *
     * @return the element instance
     */
    public Element getElement() {
        return getNewElement();
    }

    /**
     * {@inheritDoc}
     */
    public Object getObjectKey() {
        return getNewElement().getObjectKey();
    }


}
