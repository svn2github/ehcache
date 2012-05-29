/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.ElementData;

import org.terracotta.toolkit.serializer.Serializer;

import java.io.IOException;
import java.io.Serializable;

public class ValueModeHandlerSerialization implements ValueModeHandler {

  private final Serializer serializer;

  public ValueModeHandlerSerialization(Serializer serializer) {
    this.serializer = serializer;
  }

  @Override
  public Object getRealKeyObject(String portableKey) {
    try {
      return serializer.deserializeFromString(portableKey);
    } catch (IOException e) {
      return null;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Override
  public String createPortableKey(Object key) throws IOException {
    return serializer.serializeToString(key);
  }

  @Override
  public ElementData createElementData(Element element) {
    return new ElementData(element);
  }

  @Override
  public Element createElement(Object key, Serializable value) {
    return value == null ? null : ((ElementData) value).createElement(key);
  }

}
