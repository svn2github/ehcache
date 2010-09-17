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

package net.sf.ehcache.store.compound;

/**
 * HashEntry implementation using synchronized methods.
 *
 * @author Ludovic Orban
 */
class SynchronizedHashEntry extends HashEntry {

    /**
     * Volatile reference to the current value (or substitute value) for this mapping
     */
    private volatile Object element;

    /**
     * Constructs a AtomicHashEntry instance mapping the supplied key, value pair
     * and linking it to the supplied AtomicHashEntry
     *
     * @param key key for this entry
     * @param hash spread-hash for this entry
     * @param next next AtomicHashEntry in the chain
     * @param element initial value for this entry
     */
    SynchronizedHashEntry(Object key, int hash, HashEntry next, Object element) {
        super(key, hash, next, element);
    }

    /**
     * Volatile read of this entry's element reference.
     * 
     * @return mapped element
     */
    synchronized Object getElement() {
        return element;
    }

    /**
     * Volatile write of this entry's element reference.
     * 
     * @param element to map
     */
    synchronized void setElement(Object element) {
        this.element = element;
    }

    /**
     * Atomic compare-and-swap of this entry's element reference.
     * 
     * @param expect expected value
     * @param update value to install
     * @return <code>true</code> if the CAS succeeded
     */
    synchronized boolean casElement(Object expect, Object update) {
        if (this.element == expect) {
            this.element = update;
            return true;
        }
        return false;
    }
    
    /**
     * Atomic get-and-set of this entry's element reference.
     * 
     * @param element value to install
     * @return previous value
     */
    synchronized Object gasElement(Object element) {
        Object oldElement = this.element;
        this.element = element;
        return oldElement;
    }
}