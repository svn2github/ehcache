package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.SizeOfEngine;
import org.terracotta.modules.sizeof.SizeOf;

/**
 * @author Alex Snaps
 */
public class DefaultSizeOfEngine implements SizeOfEngine {

  public long sizeOf(final Object key, final Object value, final Object container) {
    return SizeOf.deepSizeOf(key, value, container);
  }
}
