/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

public class CacheLocksClient extends ClientBase {

  public CacheLocksClient(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new CacheLocksClient(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    // put locks in cache
    ReadWriteLock rwlock = toolkit.getReadWriteLock("CacheLocksClient-lock");
    cache.put(new Element("CacheLocksClient-lock", rwlock));

    // read locks from cache (make sure cache is clustered and has copyOnRead=true configured)
    final ReadWriteLock lock = (ReadWriteLock) cache.get("CacheLocksClient-lock").getValue();

    assertTrue(rwlock != lock);

    // check locks are still working
    final int COUNT = 100;
    final List<String> result = new ArrayList<String>();

    Runnable one = new Runnable() {
      public void run() {
        for (int i=0; i<COUNT ;i++) {
          lock.writeLock().lock();
          result.add("1");
          lock.writeLock().unlock();
          try { Thread.sleep(20); } catch (InterruptedException e) { /**/ }
        }
      }
    };

    Runnable two = new Runnable() {
      public void run() {
        for (int i=0; i<COUNT ;i++) {
          lock.writeLock().lock();
          result.add("2");
          lock.writeLock().unlock();
          try { Thread.sleep(20); } catch (InterruptedException e) { /**/ }
        }
      }
    };

    Thread t1 = new Thread(one);
    t1.start();
    Thread t2 = new Thread(two);
    t2.start();

    t1.join();
    t2.join();

    int oneCounter = 0;
    int twoCounter = 0;
    for (int i=0; i<COUNT *2 ;i++) {
      String  s = result.get(i);
        if (s.equals("1"))
          oneCounter++;
        else
          twoCounter++;
    }
    assertEquals(COUNT, oneCounter);
    assertEquals(COUNT, twoCounter);

  }

}
