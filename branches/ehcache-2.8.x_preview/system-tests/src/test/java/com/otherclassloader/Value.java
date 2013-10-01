/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.otherclassloader;

import java.io.Serializable;

public class Value implements Serializable {

  private final String string;

  public Value(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }

}
