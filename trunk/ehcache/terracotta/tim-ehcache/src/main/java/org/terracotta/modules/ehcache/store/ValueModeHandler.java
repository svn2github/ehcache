/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;

import org.terracotta.cache.TimestampedValue;

public interface ValueModeHandler {

  public void loadReferences();

  public Object getRealKeyObject(Object portableKey);

  public Object localGetRealKeyObject(Object portableKey);

  public Object createPortableKey(Object key);

  public TimestampedValue createTimestampedValue(Element element);

  /**
   * Returns null for null values otherwise creates an {@link Element} with the specified key-value and returns it
   */
  public Element createElement(Object key, TimestampedValue value);

}
