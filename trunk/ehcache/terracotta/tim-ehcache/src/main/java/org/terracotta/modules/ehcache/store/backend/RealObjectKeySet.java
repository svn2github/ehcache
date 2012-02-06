/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package org.terracotta.modules.ehcache.store.backend;

import org.terracotta.modules.ehcache.store.ValueModeHandler;

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
  private final boolean          local;

  public RealObjectKeySet(ValueModeHandler mode, Collection keys, boolean local) {
    this.mode = mode;
    this.keys = keys;
    this.local = local;
  }

  @Override
  public int size() {
    return keys.size();
  }

  @Override
  public boolean contains(Object o) {
    return keys.contains(mode.createPortableKey(o));
  }

  @Override
  public Iterator iterator() {
    return new KeyIterator(mode, keys.iterator(), local);
  }

  static class KeyIterator implements Iterator {

    private static final Object    NO_OBJECT = new Object();

    private final Iterator         keysIterator;
    private final ValueModeHandler mode;
    private final boolean          local;
    private Object                 next;

    private KeyIterator(ValueModeHandler mode, Iterator iterator, boolean local) {
      this.mode = mode;
      this.keysIterator = iterator;
      this.local = local;
      advance();
    }

    private void advance() {
      next = NO_OBJECT;

      while (keysIterator.hasNext()) {
        final Object real;
        if (local) {
          real = mode.localGetRealKeyObject(keysIterator.next());
          if (real == null) {
            continue;
          }
        } else {
          real = mode.getRealKeyObject(keysIterator.next());
        }

        next = real;
        return;
      }
    }

    public boolean hasNext() {
      return next != NO_OBJECT;
    }

    public Object next() {
      Object rv = next;
      if (rv == NO_OBJECT) { throw new NoSuchElementException(); }
      advance();
      return rv;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
