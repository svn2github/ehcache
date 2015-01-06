/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import java.io.Serializable;

public interface Valueable extends Serializable {
  int value();
  void setValue(int value);
}
