/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.osgi;


public class OddCacheLoader extends IncrementingCacheLoader {

  public OddCacheLoader() {
    super(false, 10000);
  }

}