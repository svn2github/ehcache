/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.ElementData;

import java.io.IOException;
import java.io.Serializable;

public interface ValueModeHandler {

  public Object getRealKeyObject(String portableKey);

  public String createPortableKey(Object key) throws IOException;

  public ElementData createElementData(Element element);

  /**
   * Returns null for null values otherwise creates an {@link Element} with the specified key-value and returns it
   */
  public Element createElement(Object key, Serializable value);

}
