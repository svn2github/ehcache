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

/**
 * 
 */
package net.sf.ehcache.store.compound;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Internal entry structure used by the {@link Segment} class.
 * 
 * @author Chris Dennis
 */
class HashEntry {
    
    /**
     * Field updater used to atomically update the volatile Element reference.
     */
    private static final AtomicReferenceFieldUpdater<HashEntry, Object> ELEMENT_UPDATER =
        AtomicReferenceFieldUpdater.newUpdater(HashEntry.class, Object.class, "element");

    /**
     * Key instance for this mapping.
     */
    protected final Object key;
    
    /**
     * Spread hash value for they key.
     */
    protected final int hash;
    
    /**
     * Reference to the next HashEntry in this chain.
     */
    protected final HashEntry next;

    /**
     * Volatile reference to the current value (or substitute value) for this mapping
     */
    private volatile Object element;

    /**
     * Constructs a HashEntry instance mapping the supplied key, value pair
     * and linking it to the supplied HashEntry
     * 
     * @param key key for this entry
     * @param hash spread-hash for this entry
     * @param next next HashEntry in the chain
     * @param element initial value for this entry
     */
    HashEntry(Object key, int hash, HashEntry next, Object element) {
        this.key = key;
        this.hash = hash;
        this.next = next;
        
        setElement(element);
    }
    
    /**
     * Volatile read of this entry's element reference.
     * 
     * @return mapped element
     */
    Object getElement() {
        return ELEMENT_UPDATER.get(this);
    }

    /**
     * Volatile write of this entry's element reference.
     * 
     * @param element to map
     */
    void setElement(Object element) {
        ELEMENT_UPDATER.set(this, element);
    }

    /**
     * Atomic compare-and-swap of this entry's element reference.
     * 
     * @param expect expected value
     * @param update value to install
     * @return <code>true</code> if the CAS succeeded
     */
    boolean casElement(Object expect, Object update) {
        return ELEMENT_UPDATER.compareAndSet(this, expect, update);
    }
    
    /**
     * Atomic get-and-set of this entry's element reference.
     * 
     * @param element value to install
     * @return previous value
     */
    Object gasElement(Object element) {
        return ELEMENT_UPDATER.getAndSet(this, element);
    }
}