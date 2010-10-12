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

package net.sf.ehcache.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Wraps a set to provide a list interface.
 * All list methods not application to set throws an
 * {@link UnsupportedOperationException}
 * @author Nabib El-Rahman
 */
public class SetWrapperList implements List {

    /**
     * Wrapped collection.
     */
    private final Collection delegate;

    /**
     * Collection to delegate to.
     * @param aDelegate delegate
     */
    public SetWrapperList(final Collection aDelegate) {
       this.delegate = aDelegate;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean add(final Object obj) {
       return this.delegate.add(obj);
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param index Index
     * @param entry Entry
     */
    public final void add(final int index, final Object entry) {
      throw new UnsupportedOperationException(
         "Delegates to set, operation not supported");
    }

    /**
     * {@inheritDoc}
     */
    public final boolean addAll(final Collection coll) {
      return this.delegate.addAll(coll);
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param index Index
     * @param coll Collection
     * @return boolean
     */
    public final boolean addAll(final int index, final Collection coll) {
       throw new UnsupportedOperationException(
          "Delegates to set, operation not supported");
    }

    /**
     * {@inheritDoc}
     */
    public final void clear() {
      this.delegate.clear();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean contains(final Object obj) {
      return this.delegate.contains(obj);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean containsAll(final Collection coll) {
      return this.delegate.containsAll(coll);
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param index Index
     * @return Object
     */
    public final Object get(final int index) {
      throw new UnsupportedOperationException(
          "Delegates to set, operation not supported");
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param object Object
     * @return integer
     */
     public final int indexOf(final Object object) {
       throw new UnsupportedOperationException(
          "Delegates to set, operation not supported");
     }

    /**
     * {@inheritDoc}
     */
    public final boolean isEmpty() {
      return this.delegate.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public final Iterator iterator() {
      return this.delegate.iterator();
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param object Object
     * @return integer
     */
    public final int lastIndexOf(final Object object) {
      throw new UnsupportedOperationException(
          "Delegates to set, operation not supported");
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @return ListIterator
     */
    public final ListIterator listIterator() {
      throw new UnsupportedOperationException(
         "Delegates to set, operation not supported");
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param index Index
     * @return ListIterator
     */
    public final ListIterator listIterator(final int index) {
       throw new UnsupportedOperationException(
          "Delegates to set, operation not supported");
    }

    /**
     * {@inheritDoc}
     */
    public final boolean remove(final Object obj) {
      return this.delegate.remove(obj);
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param index Index
     * @return Object
     */
    public final Object remove(final int index) {
      throw new UnsupportedOperationException(
        "Delegates to set, operation not supported");
    }

    /**
     * {@inheritDoc}
     */
    public final boolean removeAll(final Collection coll) {
       return this.delegate.removeAll(coll);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean retainAll(final Collection coll) {
       return this.delegate.retainAll(coll);
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param index Index
     * @param object Object
     * @return Object
     */
    public final Object set(final int index, final Object object) {
      throw new UnsupportedOperationException(
        "Delegates to set, operation not supported");
    }

    /**
     * {@inheritDoc}
     */
    public final  int size() {
      return this.delegate.size();
    }

    /**
     * Does not support List methods {@link UnsupportedOperationException}.
     * @param start Start
     * @param offset Offset
     * @return List
     */
    public final List subList(final int start, final int offset) {
       throw new UnsupportedOperationException(
          "Delegates to set, operation not supported");
    }

    /**
     * {@inheritDoc}
     */
    public final Object[] toArray() {
       return this.delegate.toArray();
    }

    /**
     * {@inheritDoc}
     */
    public final Object[] toArray(final Object[] arr) {
       return this.delegate.toArray(arr);
    }

}
