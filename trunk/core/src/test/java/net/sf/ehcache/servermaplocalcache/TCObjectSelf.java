/**
 * All content copyright 2010 (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package net.sf.ehcache.servermaplocalcache;

import java.io.Serializable;

public class TCObjectSelf implements Serializable {
  private final String key;
  private final String value;
  private final byte[] paddingBytes;
  private final long   oid;

  public TCObjectSelf(String key, String value, byte[] paddingBytes, long oid) {
    this.key = key;
    this.value = value;
    this.paddingBytes = paddingBytes;
    this.oid = oid;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public byte[] getPaddingBytes() {
    return paddingBytes;
  }

  public long getOid() {
    return oid;
  }

  @Override
  public String toString() {
    return "TCObjectSelf [oid=" + oid + ", key=" + key + ", value=" + value + "]";
  }

}
