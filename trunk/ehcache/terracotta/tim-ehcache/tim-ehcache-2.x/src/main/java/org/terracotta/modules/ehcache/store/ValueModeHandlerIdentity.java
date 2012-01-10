/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.AbstractElementData;
import net.sf.ehcache.Element;
import net.sf.ehcache.IdentityModeElementData;
import net.sf.ehcache.util.TimeUtil;

import org.terracotta.cache.TimestampedValue;

import com.tc.object.bytecode.NotClearable;

public class ValueModeHandlerIdentity implements ValueModeHandler, NotClearable {

  private final transient ClusteredStore store;

  public ValueModeHandlerIdentity(final ClusteredStore store) {
    this.store = store;
  }

  public void loadReferences() {
    // nothing to load
  }

  public Object createPortableKey(Object key) {
    return key;
  }

  public Object getRealKeyObject(Object portableKey) {
    return portableKey;
  }

  public Object localGetRealKeyObject(Object portableKey) {
    return portableKey;
  }

  public TimestampedValue createTimestampedValue(final Element element) {
    return new IdentityModeElementData(element, TimeUtil.toMillis(store.getBackend().getTimeSource().now()));
  }

  public void processStoredValue(final TimestampedValue value) {
    // no-op
  }

  public Element createElement(final Object key, final TimestampedValue value) {
    if (null == value) { return null; }

    Element element = ((AbstractElementData) value).createElement(key);
    element.setElementEvictionData(new ClusteredElementEvictionData(store, value));
    return element;
  }

}
