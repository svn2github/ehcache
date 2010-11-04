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

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * @author Alex Snaps
 */
public class StoreReplaceElementCommand implements StoreWriteCommand {

    private final Element oldElement;
    private final Element newElement;
    private final ElementValueComparator comparator;

    /**
     * Constructor
     * @param oldElement the element has supposed to be there
     * @param newElement the element to replace
     * @param comparator the comparator to be used for element comparison
     */
    public StoreReplaceElementCommand(Element oldElement, Element newElement, ElementValueComparator comparator) {
        this.oldElement = oldElement;
        this.newElement = newElement;
        this.comparator = comparator;
    }

    /**
     * {@inheritDoc}
     */
    public String getCommandName() {
        return REPLACE_ELEMENT;
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(final Store store) {
        if (!store.replace(oldElement, newElement, comparator)) {
            throw new IllegalStateException();
        }
        return true;
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
    public boolean isPut(final Object key) {
        return newElement.getKey().equals(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRemove(final Object key) {
        return false;
    }
}
