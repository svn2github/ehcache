/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package org.terracotta.modules.ehcache.store;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author cdennis
 */
class RealObjectKeySet extends AbstractSet {

  private final ValueModeHandler mode;
  private final Collection       keys;

  public RealObjectKeySet(ValueModeHandler mode, Collection keys) {
    this.mode = mode;
    this.keys = keys;
  }

  @Override
  public int size() {
    return keys.size();
  }

  @Override
  public boolean contains(Object o) {
    try {
      return keys.contains(mode.createPortableKey(o));
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public Iterator iterator() {
    return new KeyIterator(mode, keys.iterator());
  }

  static class KeyIterator implements Iterator {

    private static final Object    NO_OBJECT = new Object();

    private final Iterator         keysIterator;
    private final ValueModeHandler mode;
    private Object                 next;

    private KeyIterator(ValueModeHandler mode, Iterator iterator) {
      this.mode = mode;
      this.keysIterator = iterator;
      advance();
    }

    private void advance() {
      if (keysIterator.hasNext()) {
        final Object real;
        real = mode.getRealKeyObject((String) keysIterator.next());
        next = real;
      } else {
        next = NO_OBJECT;
      }
    }

    @Override
    public boolean hasNext() {
      return next != NO_OBJECT;
    }

    @Override
    public Object next() {
      Object rv = next;
      if (rv == NO_OBJECT) { throw new NoSuchElementException(); }
      advance();
      return rv;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
