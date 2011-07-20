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
package net.sf.ehcache.store.disk;

import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;
import net.sf.ehcache.util.VmUtils;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Internal entry structure used by the {@link net.sf.ehcache.store.disk.Segment} class.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 */
public abstract class HashEntry {

    /**
     * Key instance for this mapping.
     */
    @IgnoreSizeOf
    protected final Object key;

    /**
     * Spread hash value for they key.
     */
    protected final int hash;

    /**
     * Reference to the next HashEntry in this chain.
     */
    @IgnoreSizeOf
    protected final HashEntry next;


    /**
     * Constructs a HashEntry instance mapping the supplied key, value pair
     * and linking it to the supplied HashEntry
     *
     * @param key     key for this entry
     * @param hash    spread-hash for this entry
     * @param next    next HashEntry in the chain
     * @param element initial value for this entry
     */
    private HashEntry(Object key, int hash, HashEntry next, Object element) {
        this.key = key;
        this.hash = hash;
        this.next = next;

        setElement(element);
    }

    /**
     * Create a new HashEntry instance
     *
     * @param key the key
     * @param hash the hash of the key
     * @param next the next hashEntry
     * @param element the element
     * @return a new HashEntry instance
     */
    static HashEntry newHashEntry(Object key, int hash, HashEntry next, Object element) {
        if (VmUtils.isInGoogleAppEngine()) {
            return new SynchronizedHashEntry(key, hash, next, element);
        }
        return new AtomicHashEntry(key, hash, next, element);
    }

    /**
     * Volatile read of this entry's element reference.
     *
     * @return mapped element
     */
    abstract Object getElement();

    /**
     * Volatile write of this entry's element reference.
     *
     * @param element to map
     */
    abstract void setElement(Object element);

    /**
     * Atomic compare-and-swap of this entry's element reference.
     *
     * @param expect expected value
     * @param update value to install
     * @return <code>true</code> if the CAS succeeded
     */
    abstract boolean casElement(Object expect, Object update);

    /**
     * Atomic get-and-set of this entry's element reference.
     *
     * @param element value to install
     * @return previous value
     */
    abstract Object gasElement(Object element);

    /**
     *
     */
    static class AtomicHashEntry extends HashEntry {

        /**
         * Field updater used to atomically update the volatile Element reference.
         */
        private static final AtomicReferenceFieldUpdater<AtomicHashEntry, Object> ELEMENT_UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(AtomicHashEntry.class, Object.class, "element");

        /**
         * Volatile reference to the current value (or substitute value) for this mapping
         */
        @IgnoreSizeOf
        private volatile Object element;


        /**
         * Constructs a AtomicHashEntry instance mapping the supplied key, value pair
         * and linking it to the supplied HashEntry
         *
         * @param key     key for this entry
         * @param hash    spread-hash for this entry
         * @param next    next HashEntry in the chain
         * @param element initial value for this entry
         */
        AtomicHashEntry(Object key, int hash, HashEntry next, Object element) {
            super(key, hash, next, element);
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

    /**
     *
     */
    static class SynchronizedHashEntry extends HashEntry {

        /**
         * Volatile reference to the current value (or substitute value) for this mapping
         */
        @IgnoreSizeOf
        private volatile Object element;

        /**
         * Constructs a AtomicHashEntry instance mapping the supplied key, value pair
         * and linking it to the supplied AtomicHashEntry
         *
         * @param key     key for this entry
         * @param hash    spread-hash for this entry
         * @param next    next AtomicHashEntry in the chain
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

}
