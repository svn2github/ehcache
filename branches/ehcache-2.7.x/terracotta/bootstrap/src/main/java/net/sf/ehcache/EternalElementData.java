/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Serialized representation of an eternal {@link Element}
 */
public class EternalElementData extends ElementData {

  public EternalElementData() {
    // for serialization
  }

  public EternalElementData(final Element element) {
    super(element);

  }


  @Override
  protected void enrichElement(Element element) {
    element.setTimeToIdle(0);
    element.setTimeToLive(0);
  }


  @Override
  protected void writeAttributes(ObjectOutput oos) {
    // Do Nothing
  }


  @Override
  protected void readAttributes(ObjectInput in) {
    // Do Nothing
  }


}
