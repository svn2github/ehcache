/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.bulkops;

import java.io.Serializable;

public class DummyObject implements Serializable {
  private final String stringPart;
  private final int    intPart;

  public DummyObject(String stringKey, int intKey) {
    this.stringPart = stringKey;
    this.intPart = intKey;
  }

  public String getStringPart() {
    return stringPart;
  }

  public int getIntPart() {
    return intPart;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + intPart;
    result = prime * result + ((stringPart == null) ? 0 : stringPart.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    DummyObject other = (DummyObject) obj;
    if (intPart != other.intPart) return false;
    if (stringPart == null) {
      if (other.stringPart != null) return false;
    } else if (!stringPart.equals(other.stringPart)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "DummyObject [intPart=" + intPart + ", stringPart=" + stringPart + "]";
  }
}
