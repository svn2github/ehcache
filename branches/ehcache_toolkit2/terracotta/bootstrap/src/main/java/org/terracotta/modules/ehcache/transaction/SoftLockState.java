/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import static org.terracotta.modules.ehcache.store.SerializationHelper.deserialize;
import static org.terracotta.modules.ehcache.store.SerializationHelper.serialize;
import net.sf.ehcache.Element;

import java.io.Serializable;

public class SoftLockState implements Serializable {

  private final Element    oldElement;
  private final Element    newElement;
  private final boolean    pinned;
  private final SoftLockId softLockId;

  public SoftLockState(SoftLockId softLockId, Element oldElement, Element newElement, boolean pinned) {
    this.softLockId = softLockId;
    this.oldElement = oldElement;
    this.newElement = newElement;
    this.pinned = pinned;
  }

  public SoftLockState(SoftLockStateSerializedForm serialized) {
    this(serialized.softLockId, (Element) deserialize(serialized.oldElement),
         (Element) deserialize(serialized.newElement), serialized.pinned);
  }

  public SoftLockState newSoftLockState(Element updatedNewElement) {
    return new SoftLockState(softLockId, oldElement, updatedNewElement, pinned);
  }

  public SoftLockId getSoftLockId() {
    return softLockId;
  }

  public Element getOldElement() {
    return oldElement;
  }

  public Element getNewElement() {
    return newElement;
  }

  public boolean isPinned() {
    return pinned;
  }

  private Object writeReplace() {
    return new SoftLockStateSerializedForm(softLockId, serialize(oldElement), serialize(newElement), pinned);
  }

  @Override
  public String toString() {
    return "SoftLockState [oldElement=" + oldElement + ", newElement=" + newElement + ", pinned=" + pinned + "]";
  }

  private static class SoftLockStateSerializedForm implements Serializable {

    private final byte[]     oldElement;
    private final byte[]     newElement;
    private final boolean    pinned;
    private final SoftLockId softLockId;

    public SoftLockStateSerializedForm(SoftLockId softLockId, byte[] oldElement, byte[] newElement, boolean pinned) {
      this.softLockId = softLockId;
      this.oldElement = oldElement;
      this.newElement = newElement;
      this.pinned = pinned;
    }

    private Object readResolve() {
      return new SoftLockState(this);
    }

  }

}
