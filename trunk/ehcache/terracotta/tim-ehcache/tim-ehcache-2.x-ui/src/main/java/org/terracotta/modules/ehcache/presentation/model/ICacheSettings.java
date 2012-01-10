/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

public interface ICacheSettings extends IMutableCacheSettings {
  String getMemoryStoreEvictionPolicy();

  boolean isDiskPersistent();

  boolean isEternal();

  boolean isOverflowToDisk();
}
