/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class UnmodifiableCollectionWrapper<T> extends AbstractList<T> {

  private final Collection<? extends T> delegate;

  public UnmodifiableCollectionWrapper(Collection<? extends T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean contains(Object obj) {
    return this.delegate.contains(obj);
  }

  @Override
  public boolean containsAll(Collection<?> coll) {
    return this.delegate.containsAll(coll);
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
    return this.delegate.isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return new UnmodifiableIterator(this.delegate.iterator());
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
    return this.delegate.size();
  }

  @Override
  public List<T> subList(int paramInt1, int paramInt2) {
    throw new UnsupportedOperationException("Delegates to set, operation not supported");
  }

  @Override
  public Object[] toArray() {
    return this.delegate.toArray();
  }

  @Override
  public <E> E[] toArray(E[] arr) {
    return this.delegate.toArray(arr);
  }

  private static class UnmodifiableIterator<T> implements Iterator<T> {

    private final Iterator<? extends T> delegate;

    public UnmodifiableIterator(Iterator<? extends T> delegate) {
      this.delegate = delegate;
    }

    public boolean hasNext() {
      return delegate.hasNext();
    }

    public T next() {
      return delegate.next();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
