/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.event.EventListenerList;

/**
 * Base class for creating models from collections of MBeans contained in L1s, handling all of the grunt work of
 * invoking requests or retrieving attribute sets across of collections of beans. All TIM presentation models should
 * extends this type.
 */

public abstract class BaseMBeanModel implements NotificationListener, ClientConnectionListener {
  protected final IClusterModel     clusterModel;
  protected final ClusterListener   clusterListener;
  protected final EventListenerList listenerList;
  protected final Set<ObjectName>   onSet;

  private static final Random       RANDOM      = new Random();

  private static final Object[]     NULL_PARAMS = {};
  private static final String[]     NULL_SIGS   = {};

  private static final ObjectName   MBEAN_SERVER_DELEGATE;
  static {
    try {
      MBEAN_SERVER_DELEGATE = new ObjectName("JMImplementation:type=MBeanServerDelegate");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected BaseMBeanModel(IClusterModel clusterModel) {
    this.clusterModel = clusterModel;
    this.clusterListener = new ClusterListener(clusterModel);
    this.listenerList = new EventListenerList();
    this.onSet = new HashSet<ObjectName>();
  }

  public void startup() {
    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.isReady()) {
      init();
    } else {
      suspend();
    }
  }

  public void clientConnected(IClient client) {/**/
  }

  public void clientDisconnected(IClient client) {/**/
  }

  public abstract void handleNotification(Notification notif, Object data);

  public abstract void init();

  public abstract void suspend();

  public void reset() {
    /**/
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public IServer getActiveCoordinator() {
    return clusterModel.getActiveCoordinator();
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel, false);
    }

    @Override
    protected void handleReady() {
      if (clusterModel.isReady()) {
        init();
      } else {
        suspend();
      }
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (newActive != null) {
        reset();
      }
    }
  }

  protected void addListeners() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      try {
        activeCoord.addNotificationListener(MBEAN_SERVER_DELEGATE, this);
        activeCoord.addClientConnectionListener(this);
      } catch (Exception e) {
        /**/
      }
    }
  }

  protected void removeListeners() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      try {
        activeCoord.removeNotificationListener(MBEAN_SERVER_DELEGATE, this);
        activeCoord.removeClientConnectionListener(this);
      } catch (Exception e) {
        /**/
      }
    }
  }

  public ObjectName getRandomBean() {
    return getRandomBean(onSet);
  }

  public int beanCount() {
    return onSet.size();
  }

  /*
   * Amazingly, ConcurrentHashMap.KeySet isn't serializable!
   */
  private Set<ObjectName> newSet(Collection<ObjectName> collection) {
    return new HashSet<ObjectName>(collection);
  }

  public ObjectName getRandomBean(Set<ObjectName> targets) {
    if (targets != null) {
      ObjectName[] beanArray = targets.toArray(new ObjectName[0]);
      if (beanArray.length > 0) { return beanArray[RANDOM.nextInt(beanArray.length)]; }
    }
    return null;
  }

  public Object invokeOnce(String operation) {
    return invokeOnce(operation, NULL_PARAMS, NULL_SIGS);
  }

  public Object invokeOnce(ObjectName target, String operation) {
    return invokeOnce(target, operation, NULL_PARAMS, NULL_SIGS);
  }

  public Object invokeOnce(String operation, Object[] params, String[] sigs) {
    return invokeOnce(getRandomBean(), operation, params, sigs);
  }

  public Object invokeOnce(ObjectName target, String operation, Object[] params, String[] sigs) {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null && target != null) {
      Map<ObjectName, Object> response = activeCoord.invoke(Collections.singleton(target), operation,
                                                            Integer.MAX_VALUE, TimeUnit.SECONDS, params, sigs);
      if (response != null) { return response.get(target); }
    }
    return null;
  }

  public Map<ObjectName, Object> invokeAll(String operation) {
    return invokeAll(onSet, operation);
  }

  public Map<ObjectName, Object> invokeAll(Set<ObjectName> targets, String operation) {
    return invokeAll(targets, operation, NULL_PARAMS, NULL_SIGS);
  }

  public Map<ObjectName, Object> invokeAll(String operation, Object[] params, String[] sigs) {
    return invokeAll(onSet, operation, params, sigs);
  }

  public Map<ObjectName, Object> invokeAll(Set<ObjectName> targets, String operation, Object[] params, String[] sigs) {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      Map<ObjectName, Object> result = activeCoord.invoke(newSet(targets), operation, params, sigs);
      return result;
    }
    return null;
  }

  public Map<ObjectName, Map<String, Object>> getAttributes(Set<String> attrSet) {
    return getAttributes(onSet, attrSet);
  }

  public Map<String, Object> getAttributes(ObjectName target, Set<String> attrSet) {
    Map<ObjectName, Map<String, Object>> mapping = getAttributes(Collections.singleton(target), attrSet);
    return mapping != null ? mapping.get(target) : null;
  }

  public Map<ObjectName, Map<String, Object>> getAttributes(Set<ObjectName> targets, Set<String> attrSet) {
    Map<ObjectName, Map<String, Object>> result = new HashMap<ObjectName, Map<String, Object>>();
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      Map<ObjectName, Set<String>> request = new HashMap<ObjectName, Set<String>>();
      for (ObjectName objectName : targets) {
        request.put(objectName, attrSet);
      }
      result = activeCoord.getAttributeMap(request);
    }
    return result;
  }

  public Map<ObjectName, Object> getAttribute(String attr) {
    return getAttribute(onSet, attr);
  }

  public Object getAttribute(ObjectName target, String attr) {
    Map<ObjectName, Object> mapping = getAttribute(Collections.singleton(target), attr);
    return mapping != null ? mapping.get(target) : null;
  }

  public Map<ObjectName, Object> getAttribute(Set<ObjectName> targets, String attr) {
    Map<ObjectName, Object> result = new HashMap<ObjectName, Object>();
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      Map<ObjectName, Set<String>> request = new HashMap<ObjectName, Set<String>>();
      for (ObjectName objectName : targets) {
        request.put(objectName, Collections.singleton(attr));
      }
      Map<ObjectName, Map<String, Object>> response = activeCoord.getAttributeMap(request);
      if (response != null) {
        for (Entry<ObjectName, Map<String, Object>> entry : response.entrySet()) {
          Map<String, Object> mapping = entry.getValue();
          if (mapping != null) {
            result.put(entry.getKey(), mapping.get(attr));
          }
        }
      }
    }
    return result;
  }

  public void setAttribute(String attr, Object value) throws Exception {
    setAttribute(onSet, attr, value);
  }

  public void safeSetAttribute(String attr, Object value) {
    safeSetAttribute(onSet, attr, value);
  }

  public void safeSetAttribute(Set<ObjectName> targets, String attr, Object value) {
    try {
      setAttribute(targets, attr, value);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setAttribute(ObjectName on, String attr, Object value) throws Exception {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.setAttribute(on, attr, value);
    }
  }

  public void setAttribute(Set<ObjectName> targets, String attr, Object value) throws Exception {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.setAttribute(newSet(targets), attr, value);
    }
  }

  public void safeSetAttribute(ObjectName on, String attr, Object value) {
    try {
      setAttribute(on, attr, value);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean addNotificationListener(ObjectName on, NotificationListener listener) {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      try {
        activeCoord.addNotificationListener(on, listener);
        return true;
      } catch (Exception e) {
        /**/
      }
    }
    return false;
  }

  public boolean removeNotificationListener(ObjectName on, NotificationListener listener) {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      try {
        activeCoord.removeNotificationListener(on, listener);
        return true;
      } catch (Exception e) {
        /**/
      }
    }
    return false;
  }

  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();
    onSet.clear();
  }
}
