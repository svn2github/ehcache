/**
 * All content copyright 2010 (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package net.sf.ehcache.servermaplocalcache;

import java.util.concurrent.atomic.AtomicLong;

public class TCObjectSelfFactory {

  private static final int VALUE_PADDING_BYTES = 1024;
  private final AtomicLong oidCounter          = new AtomicLong();

  public TCObjectSelf newTCObjectSelf() {
    long oid = oidCounter.incrementAndGet();
    return new TCObjectSelf(getKeyForId(oid), getValueForId(oid), new byte[VALUE_PADDING_BYTES], oid);
  }

  public String getValueForId(long oid) {
    return "value-" + oid;
  }

  public String getKeyForId(long oid) {
    return "key-" + oid;
  }

  public AtomicLong getOidCounter() {
    return oidCounter;
  }
}
