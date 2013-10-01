/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.osgi;

public class EvenCacheLoader extends IncrementingCacheLoader {

  public EvenCacheLoader() {
    super(true, 20000);
  }

}