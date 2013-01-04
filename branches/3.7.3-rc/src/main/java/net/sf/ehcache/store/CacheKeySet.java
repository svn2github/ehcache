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

package net.sf.ehcache.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A Set of keys that will encapsulate keys present in a Cache.
 * It will <em>mostly</em> behave as an immutable {@link java.util.Set}, but for its {@link #size} method
 *
 * @param <E> the type of elements maintained by this set
 * @author Alex Snaps
 */
public class CacheKeySet<E> implements Set<E> {

    private static final Iterator EMPTY_ITERATOR = new Iterator() {
        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new UnsupportedOperationException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    private final Collection<E>[] keySets;

    /**
     * Create a new Set for all tiers in the cache.
     * Generally, you'd pass the authority's keySet, and higher layer pinned keySets
     *
     * @param keySets an array of keySets
     */
    public CacheKeySet(final Collection<E>... keySets) {
        this.keySets = keySets;
    }

    /**
     * Sums the size of all sets wrapped by this one, so this will not account for duplicated keys. Like for in-memory pinned keys, that
     * might also present in lower tiers: e.g. DiskStore when its capacity isn't reached.
     * @return the sum of all keySet sizes
     */
    public int size() {
        int size = 0;
        for (Collection keySet : keySets) {
            size += keySet.size();
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {

        for (Collection keySet : keySets) {
            if (!keySet.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(final Object o) {

        for (Collection keySet : keySets) {
            if (keySet.contains(o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<E> iterator() {
        return new KeySetIterator();
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray() {
        List<E> list = new ArrayList<E>();
        for (E e : this) {
            list.add(e);
        }
        return list.toArray();
    }

    /**
     * {@inheritDoc}
     */
    public <T> T[] toArray(final T[] a) {
        List<E> list = new ArrayList<E>();
        for (E e : this) {
            list.add(e);
        }
        return list.toArray(a);
    }

    /**
     * You can't add to this set, will throw!
     * @throws UnsupportedOperationException
     */
    public boolean add(final Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * You can't remove from this set, will throw!
     * @throws UnsupportedOperationException
     */
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(final Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * You can't add to this set, will throw!
     * @throws UnsupportedOperationException
     */
    public boolean addAll(final Collection c) {
        throw new UnsupportedOperationException();
    }

    /**
     * You can't remove from this set, will throw!
     * @throws UnsupportedOperationException
     */
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * You can't remove from this set, will throw!
     * @throws UnsupportedOperationException
     */
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * You can't remove from this set, will throw!
     * @throws UnsupportedOperationException
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * An iterator that will iterate over all keySets, avoiding duplicate entries
     */
    private final class KeySetIterator implements Iterator<E> {

        private Iterator<E> currentIterator;
        private int index = 0;
        private E next;
        private E current;

        private KeySetIterator() {
            if (keySets.length == 0) {
                this.currentIterator = EMPTY_ITERATOR;
            } else {
                this.currentIterator = keySets[0].iterator();
            }
            advance();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return next != null;
        }

        /**
         * {@inheritDoc}
         */
        public E next() {
            current = next;
            advance();
            return current;
        }

        private void advance() {
            next = null;
            while (next == null) {
                if (currentIterator.hasNext()) {
                    next = currentIterator.next();
                    for (int i = 0; i < index; i++) {
                        if (keySets[i].contains(next)) {
                            next = null;
                        }
                    }
                } else {
                    next = null;
                    if (++index < keySets.length) {
                        currentIterator = keySets[index].iterator();
                    } else {
                        return;
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
