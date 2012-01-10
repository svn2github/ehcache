/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

class AggregateIterator<T> implements Iterator<T> {

  private final Iterator<Iterator<? extends T>> iterators;
  private Iterator<? extends T>                 currentIterator;

  AggregateIterator(Collection<Iterator<? extends T>> iterators) {
    this.iterators = iterators.iterator();
    while (this.iterators.hasNext()) {
      this.currentIterator = this.iterators.next();
      if (this.currentIterator.hasNext()) { return; }
    }
  }

  public boolean hasNext() {
    if (currentIterator == null) { return false; }

    if (currentIterator.hasNext()) {
      return true;
    } else {
      while (iterators.hasNext()) {
        currentIterator = iterators.next();
        if (currentIterator.hasNext()) { return true; }
      }
    }

    return false;
  }

  public T next() {
    if (currentIterator == null) { throw new NoSuchElementException(); }

    if (currentIterator.hasNext()) {
      return currentIterator.next();
    } else {
      while (iterators.hasNext()) {
        currentIterator = iterators.next();

        if (currentIterator.hasNext()) { return currentIterator.next(); }
      }
    }

    throw new NoSuchElementException();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}