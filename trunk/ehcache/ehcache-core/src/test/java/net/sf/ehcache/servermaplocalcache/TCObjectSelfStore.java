/**
 * All content copyright 2010 (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package net.sf.ehcache.servermaplocalcache;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCObjectSelfStore {

  private static final Logger          logger = LoggerFactory.getLogger(TCObjectSelfStore.class);

  private final Set<Long>              oids   = new HashSet<Long>();
  private final Ehcache                cache;
  private final ReentrantReadWriteLock lock   = new ReentrantReadWriteLock();

  public TCObjectSelfStore(Ehcache cache) {
    this.cache = cache;
  }

  public TCObjectSelf getById(Long oid) {
    long timePrev = System.currentTimeMillis();
    long startTime = timePrev;
    while (true) {
      TCObjectSelf rv = null;
      lock.readLock().lock();
      try {
        if (!oids.contains(oid)) {
          return null;
        }
        Element element = cache.get(oid);
        if (element != null) {
          String key = (String) element.getObjectValue();
          if (key != null) {
            Element valueElement = cache.get(key);
            if (valueElement != null) {
              rv = (TCObjectSelf) valueElement.getObjectValue();
            }
          }
        }
      } finally {
        lock.readLock().unlock();
      }
      if (rv != null) {
        return rv;
      }

      long currTime = System.currentTimeMillis();
      if ((currTime - timePrev) > (15 * 1000)) {
        timePrev = currTime;
        logger.info("Still waiting to get the Object from local cache, ObjectID=" + oid + " , times spent="
            + ((currTime - startTime) / 1000) + "seconds");
      }
      waitUntilNotified();
    }
  }

  private void waitUntilNotified() {
    boolean isInterrupted = false;
    try {
      synchronized (this) {
        this.wait(1000);
      }
    } catch (InterruptedException e) {
      isInterrupted = true;
    } finally {
      if (isInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void addTCObjectSelf(TCObjectSelf tcoSelf) {
    lock.writeLock().lock();
    try {
      this.oids.add(tcoSelf.getOid());
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void removeTCObjectSelf(TCObjectSelf tcoSelf) {
    synchronized (this) {
      lock.writeLock().lock();
      try {
        this.oids.remove(tcoSelf.getOid());
      } finally {
        lock.writeLock().unlock();
      }
      this.notifyAll();
    }
  }
}
