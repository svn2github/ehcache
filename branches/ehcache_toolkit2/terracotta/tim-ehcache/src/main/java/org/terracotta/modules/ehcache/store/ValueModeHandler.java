/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;

import java.io.IOException;

public interface ValueModeHandler {

  public Object getRealKeyObject(Object portableKey);

  public Object createPortableKey(Object key) throws IOException;

  public Object createElementData(Element element);

  /**
   * Returns null for null values otherwise creates an {@link Element} with the specified key-value and returns it
   */
  public Element createElement(Object key, Object value);

}
