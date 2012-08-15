/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.SoftLockID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Ludovic Orban
 */
public class ClusteredSoftLockID {

  private volatile transient SoftLockID softLockId;
  private final byte[] softLockIdSerializedForm;

  public ClusteredSoftLockID(SoftLockID softLockId) {
    this.softLockId = softLockId;
    this.softLockIdSerializedForm = serialize(softLockId);
  }

  public SoftLockID getSoftLockId() {
    if (softLockId == null) {
      softLockId = (SoftLockID)deserialize(softLockIdSerializedForm);
    }
    return softLockId;
  }

  @Override
  public int hashCode() {
    return getSoftLockId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClusteredSoftLockID) {
      ClusteredSoftLockID other = (ClusteredSoftLockID)obj;
      return getSoftLockId().equals(other.getSoftLockId());
    }
    return false;
  }


  private static byte[] serialize(Object obj) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("error serializing " + obj);
    }
  }

  private static Object deserialize(byte[] bytes) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      Object obj = ois.readObject();
      ois.close();
      return obj;
    } catch (Exception e) {
      throw new RuntimeException("error deserializing " + bytes);
    }
  }

}
