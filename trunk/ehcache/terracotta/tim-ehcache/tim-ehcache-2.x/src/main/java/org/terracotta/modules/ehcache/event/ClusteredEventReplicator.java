package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.AbstractElementData;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.IdentityModeElementData;
import net.sf.ehcache.SerializationModeElementData;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners.ElementCreationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.cache.serialization.DsoSerializationStrategy;
import org.terracotta.cache.serialization.SerializationStrategy;
import org.terracotta.cluster.TerracottaClusterInfo;
import org.terracotta.modules.ehcache.store.ElementSerializationStrategy;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClusteredEventReplicator implements CacheEventListener {
  /**
   * Using this to prevent the memory manager to reclaim ClusteredEventReplicator in between the load time by the
   * cache's
   * initialization and the actual usage with DMI
   */
  private static final Collection                             UNFLUSHABLE_STATE                    = Collections
                                                                                                       .synchronizedCollection(new HashSet());

  private static final Logger                                 LOG                                  = LoggerFactory
                                                                                                       .getLogger(ClusteredEventReplicator.class
                                                                                                           .getName());
  private static final int                                    MS_WAIT_FOR_TRANSIENT_INITIALISATION = 5000;

  private final TerracottaConfiguration.ValueMode             valueMode;
  private final SerializationStrategy                         dsoSerialization;
  private final SerializationStrategy                         elementSerialization;

  private transient volatile ReentrantReadWriteLock           transientLock;
  private transient volatile ReentrantReadWriteLock.WriteLock transientWriteLock;
  private transient volatile Condition                        transientCondition;
  private transient volatile ReentrantReadWriteLock.ReadLock  transientReadLock;

  private transient Ehcache                                   cache;

  public ClusteredEventReplicator(Ehcache cache, TerracottaConfiguration tcConfig) {
    this.valueMode = tcConfig.getValueMode();

    switch (valueMode) {
      case IDENTITY:
        this.dsoSerialization = null;
        this.elementSerialization = null;
        break;
      case SERIALIZATION:
        this.dsoSerialization = new DsoSerializationStrategy();
        this.elementSerialization = new ElementSerializationStrategy(tcConfig.isCompressionEnabled());
        break;
      default:
        throw new UnsupportedOperationException("Value mode " + valueMode + " isn't supported");
    }

    initializeOnLoad();
    initializeTransients(cache);
  }

  public synchronized void initializeOnLoad() {
    if (null == transientLock) {
      transientLock = new ReentrantReadWriteLock();
      transientWriteLock = transientLock.writeLock();
      transientCondition = transientWriteLock.newCondition();
      transientReadLock = transientLock.readLock();
    }
  }

  /**
   * This method is used for common on-load and instantiation logic. We can't rely on the standard DSO on-load feature
   * since roots of ClusteredEventDispatcher are explicitly created in
   * {@link org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory} through a ManagerUtil call, as
   * opposed to the regular root declaration in DSO instrumented classes.
   * <p/>
   * This approach is needed for 'express' features since none of the application context classes can be instrumented by
   * Terracotta due to the lack of a boot jar.
   */
  public void initializeTransients(Ehcache theCache) {
    transientWriteLock.lock();
    try {
      this.cache = theCache;

      UNFLUSHABLE_STATE.add(this);

      transientCondition.signalAll();
    } finally {
      transientWriteLock.unlock();
    }
  }

  public void notifyElementRemoved(Ehcache theCache, Element element) throws CacheException {
    dmiNotifyElementRemoved(getKeyForDMI(element), getElementDataForDMI(element), getClientID());
  }

  public void notifyElementPut(Ehcache theCache, Element element) throws CacheException {
    dmiNotifyElementPut(getKeyForDMI(element), getElementDataForDMI(element), getClientID());
  }

  public void notifyElementUpdated(Ehcache theCache, Element element) throws CacheException {
    dmiNotifyElementUpdated(getKeyForDMI(element), getElementDataForDMI(element), getClientID());
  }

  public void notifyElementExpired(Ehcache theCache, Element element) throws CacheException {
    dmiNotifyElementExpired(getKeyForDMI(element), getElementDataForDMI(element), getClientID());
  }

  public void notifyElementEvicted(Ehcache theCache, Element element) throws CacheException {
    dmiNotifyElementEvicted(getKeyForDMI(element), getElementDataForDMI(element), getClientID());
  }

  public void notifyRemoveAll(Ehcache theCache) {
    dmiNotifyRemoveAll(getClientID());
  }

  private String getClientID() {
    return new TerracottaClusterInfo().getCurrentNode().getId();
  }

  public void dispose() {
    // no-op
  }

  @Override
  public ClusteredEventReplicator clone() throws CloneNotSupportedException {
    return (ClusteredEventReplicator) super.clone();
  }

  private Object getKeyForDMI(Element element) throws CacheException {
    switch (valueMode) {
      case IDENTITY:
        return element.getObjectKey();
      case SERIALIZATION:
        try {
          return dsoSerialization.serialize(element.getObjectKey());
        } catch (IOException e) {
          throw new CacheException("Unexpected error while serializing the key " + element.getObjectKey(), e);
        }
      default:
        throw new UnsupportedOperationException("Value mode " + valueMode + " isn't supported");
    }
  }

  private Object getElementDataForDMI(Element element) throws CacheException {
    AbstractElementData elementData;
    switch (valueMode) {
      case IDENTITY:
        return new IdentityModeElementData(element, Integer.MIN_VALUE);
      case SERIALIZATION:
        elementData = new SerializationModeElementData(element);
        try {
          return elementSerialization.serialize(elementData);
        } catch (IOException e) {
          throw new CacheException("Unexpected error while serializing the value " + element.getObjectValue(), e);
        }
      default:
        throw new UnsupportedOperationException("Value mode " + valueMode + " isn't supported");
    }

  }

  private Object getKeyFromDMI(Object key, ClassLoader loader) throws CacheException {
    switch (valueMode) {
      case IDENTITY:
        return key;
      case SERIALIZATION:
        try {
          return dsoSerialization.deserialize((byte[]) key, loader);
        } catch (Exception e) {
          throw new CacheException("Unexpected error while deserializing a key from a DMI call.", e);
        }
    }

    throw new UnsupportedOperationException("Value mode " + valueMode + " isn't supported");
  }

  private Element getElementFromDMI(Object key, Object element, ClassLoader loader) throws CacheException {
    AbstractElementData elementData;
    switch (valueMode) {
      case IDENTITY:
        elementData = (AbstractElementData) element;
        break;
      case SERIALIZATION:
        try {
          elementData = (AbstractElementData) elementSerialization.deserialize((byte[]) element, loader);
        } catch (Exception e) {
          throw new CacheException("Unexpected error while deserializing an element from a DMI call.", e);
        }
        break;
      default:
        throw new UnsupportedOperationException("Value mode " + valueMode + " isn't supported");
    }

    return elementData.createElement(getKeyFromDMI(key, loader));
  }

  private boolean isRemote(String clientID) {
    return !getClientID().equals(clientID);
  }

  // This wait approach is required since TerracottaClusteredInstanceFactory will fault in a this
  // ClusteredEventReplicator
  // and only later call initializeTransients() on it. There's a gap there where DMI can come in while a replicator
  // is still uninitialized for its current node. We're waiting for a little while if that's the case to at least give
  // it
  // a chance to do the initialization. It will not wait forever though. If the initialization didn't execute in time,
  // the DMI methods will detect this and print out a warning.

  private void giveTransientInitializationTime() {
    transientReadLock.lock();
    try {
      if (cache != null) { return; }
    } finally {
      transientReadLock.unlock();
    }

    transientWriteLock.lock();
    try {
      if (cache != null) { return; }

      try {
        transientCondition.await(MS_WAIT_FOR_TRANSIENT_INITIALISATION, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } finally {
      transientWriteLock.unlock();
    }
  }

  private Ehcache getCache() {
    giveTransientInitializationTime();

    transientReadLock.lock();
    try {
      return cache;
    } finally {
      transientReadLock.unlock();
    }
  }

  private boolean isInitialized() {
    return getCache() != null;
  }

  public void dmiNotifyElementPut(Object key, Object elementData, String clientID) throws CacheException {
    if (isRemote(clientID)) {
      if (!isInitialized()) {
        if (LOG.isWarnEnabled()) LOG
            .warn("Received a replicated put event for an uninitialized cache event replicator, events will be ignored while initialization is in progress.");
        return;
      }

      getCache().getCacheEventNotificationService().notifyElementPut(new CreateCallback(key, elementData), true);
    }
  }

  public void dmiNotifyElementUpdated(Object key, Object elementData, String clientID) {
    if (isRemote(clientID)) {
      if (!isInitialized()) {
        if (LOG.isWarnEnabled()) LOG
            .warn("Received a replicated update event for an uninitialized cache event replicator, events will be ignored while initialization is in progress.");
        return;
      }

      getCache().getCacheEventNotificationService().notifyElementUpdated(new CreateCallback(key, elementData), true);
    }
  }

  public void dmiNotifyElementExpired(Object key, Object elementData, String clientID) {
    if (isRemote(clientID)) {
      if (!isInitialized()) {
        if (LOG.isWarnEnabled()) LOG
            .warn("Received a replicated expiration event for an uninitialized cache event replicator, events will be ignored while initialization is in progress.");
        return;
      }

      getCache().getCacheEventNotificationService().notifyElementExpiry(new CreateCallback(key, elementData), true);
    }
  }

  public void dmiNotifyElementEvicted(Object key, Object elementData, String clientID) {
    if (isRemote(clientID)) {
      if (!isInitialized()) {
        if (LOG.isWarnEnabled()) LOG
            .warn("Received a replicated eviction event for an uninitialized cache event replicator, events will be ignored while initialization is in progress.");
        return;
      }

      getCache().getCacheEventNotificationService().notifyElementEvicted(new CreateCallback(key, elementData), true);
    }
  }

  public void dmiNotifyElementRemoved(Object key, Object elementData, String clientID) throws CacheException {
    if (isRemote(clientID)) {
      if (!isInitialized()) {
        if (LOG.isWarnEnabled()) LOG
            .warn("Received a replicated remove event for an uninitialized cache event replicator, events will be ignored while initialization is in progress.");
        return;
      }

      getCache().getCacheEventNotificationService().notifyElementRemoved(new CreateCallback(key, elementData), true);
    }
  }

  public void dmiNotifyRemoveAll(String clientID) {
    if (isRemote(clientID)) {
      if (!isInitialized()) {
        if (LOG.isWarnEnabled()) LOG
            .warn("Received a replicated removeAll event for an uninitialized cache event replicator, events will be ignored while initialization is in progress.");
        return;
      }

      getCache().getCacheEventNotificationService().notifyRemoveAll(true);
    }
  }

  private class CreateCallback implements ElementCreationCallback {

    private final Object key;
    private final Object elementData;

    CreateCallback(Object key, Object elementData) {
      this.key = key;
      this.elementData = elementData;
    }

    public Element createElement(ClassLoader loader) {
      return getElementFromDMI(key, elementData, new ElementLoader(loader));
    }
  }

  private class ElementLoader extends ClassLoader {
    private final ClassLoader listenerLoader;

    ElementLoader(ClassLoader listenerLoader) {
      super(null);
      this.listenerLoader = listenerLoader;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      try {
        return listenerLoader.loadClass(name);
      } catch (ClassNotFoundException cnfe) {
        //
      }

      // fallback to this loader
      return getClass().getClassLoader().loadClass(name);
    }
  }

}
