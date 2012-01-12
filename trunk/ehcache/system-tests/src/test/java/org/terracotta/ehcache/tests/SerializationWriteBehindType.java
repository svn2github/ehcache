package org.terracotta.ehcache.tests;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

public class SerializationWriteBehindType implements Serializable {
  private final String value;
  private final Date moment;
  private final SerializationWriteBehindSubType subType;

  public SerializationWriteBehindType(String value) {
    this.value = value;
    this.moment = new Date();
    this.subType = new SerializationWriteBehindSubType(new BigInteger(String.valueOf(value.hashCode())));
  }

  public String toString() {
    return value + ", " + moment + ", " + subType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SerializationWriteBehindType that = (SerializationWriteBehindType) o;

    if (value != null ? !value.equals(that.value) : that.value != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return value != null ? value.hashCode() : 0;
  }

  public static class SerializationWriteBehindSubType implements Serializable {
    private final BigInteger value;

    public SerializationWriteBehindSubType(BigInteger value) {
      this.value = value;
    }

    public BigInteger getValue() {
      return value;
    }
  }
}
