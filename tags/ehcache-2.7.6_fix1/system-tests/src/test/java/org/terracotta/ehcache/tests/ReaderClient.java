/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.classloader.ClassLoaderAwareCache;

import java.util.Iterator;
import java.util.List;

public class ReaderClient extends OtherClassloaderClient {

  public static void main(String[] args) {
    new ReaderClient(args).run();
  }

  public ReaderClient(String[] args) {
    super(args);
  }

  protected void test(Cache c) throws Throwable {
    Ehcache cache = new ClassLoaderAwareCache(c, createClassLoader());

    List keys = cache.getKeys();
    Iterator iter = keys.iterator();
    while (iter.hasNext()) {
      Object key = iter.next();
      System.out.println(cache.get(key));
    }
  }
}
