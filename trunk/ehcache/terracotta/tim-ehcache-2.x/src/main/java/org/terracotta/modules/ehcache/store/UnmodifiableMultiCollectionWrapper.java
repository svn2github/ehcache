package org.terracotta.modules.ehcache.store;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class UnmodifiableMultiCollectionWrapper<T> extends AbstractList<T> {

  private final Collection<Collection<? extends T>> delegates;

  public UnmodifiableMultiCollectionWrapper(Collection<? extends T>... delegates) {
    this.delegates = Collections.unmodifiableList(Arrays.asList(delegates));
  }

  @Override
  public boolean contains(Object obj) {
    for (Collection<? extends T> c : delegates) {
      if (c.contains(obj)) { return true; }
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> coll) {
    for (Collection<? extends T> c : delegates) {
      if (c.containsAll(coll)) { return true; }
    }
    for (Object o : coll) {
      if (!contains(o)) { return false; }
    }
    return true;
  }

  @Override
  public T get(int position) {
    throw new UnsupportedOperationException("Delegates to set, operation not supported");
  }

  @Override
  public int indexOf(Object obj) {
    throw new UnsupportedOperationException("Delegates to set, operation not supported");
  }

  @Override
  public boolean isEmpty() {
    for (Collection<?> c : delegates) {
      if (!c.isEmpty()) { return false; }
    }
    return true;
  }

  @Override
  public Iterator<T> iterator() {
    Collection<Iterator<? extends T>> iterators = new ArrayList<Iterator<? extends T>>(delegates.size());
    for (Collection<? extends T> c : delegates) {
      iterators.add(c.iterator());
    }
    return new AggregateIterator<T>(iterators);
  }

  @Override
  public int lastIndexOf(Object paramObject) {
    throw new UnsupportedOperationException("Delegates to set, operation not supported");
  }

  @Override
  public ListIterator<T> listIterator() {
    throw new UnsupportedOperationException("Delegates to set, operation not supported");
  }

  @Override
  public ListIterator<T> listIterator(int paramInt) {
    throw new UnsupportedOperationException("Delegates to set, operation not supported");
  }

  @Override
  public int size() {
    long totalSize = 0;
    for (Collection<?> c : delegates) {
      totalSize += c.size();
    }
    if (totalSize > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) totalSize;
    }
  }

  @Override
  public List<T> subList(int paramInt1, int paramInt2) {
    throw new UnsupportedOperationException("Delegates to set, operation not supported");
  }
}
