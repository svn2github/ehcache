/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.ElementData;

import org.terracotta.toolkit.serialization.ToolkitSerializer;

import java.io.Serializable;

public class ValueModeHandlerSerialization implements ValueModeHandler {

  private final ToolkitSerializer toolkitSerializer;

  public ValueModeHandlerSerialization(ToolkitSerializer toolkitSerializer) {
    this.toolkitSerializer = toolkitSerializer;
  }

  @Override
  public Object getRealKeyObject(String portableKey, boolean localOnly) {
    if (localOnly) {
      return toolkitSerializer.deserializeFromStringLocally(portableKey);
    } else {
      return toolkitSerializer.deserializeFromString(portableKey);
    }
  }

  @Override
  public String createPortableKey(Object key) {
    return toolkitSerializer.serializeToString(key);
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
