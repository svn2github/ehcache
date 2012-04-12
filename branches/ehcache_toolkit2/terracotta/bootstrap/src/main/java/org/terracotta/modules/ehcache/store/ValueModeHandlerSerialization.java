/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.ElementData;

import java.io.IOException;
import java.io.Serializable;

public class ValueModeHandlerSerialization implements ValueModeHandler {

  @Override
  public Object getRealKeyObject(Object portableKey) {
    try {
      return SerializationHelper.deserializeStringKey((String) portableKey);
    } catch (IOException e) {
      return null;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  public Object createPortableKey(Object key) throws IOException {
    return SerializationHelper.serializeToString(key);
  }

  @Override
  public ElementData createElementData(Element element) {
    return new ElementData(element);
  }

  @Override
  public Element createElement(Object key, Serializable value) {
    return ((ElementData) value).createElement(key);
  }

}
