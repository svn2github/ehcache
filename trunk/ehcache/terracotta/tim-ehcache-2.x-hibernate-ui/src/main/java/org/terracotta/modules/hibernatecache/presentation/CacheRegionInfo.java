/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import net.sf.ehcache.hibernate.management.api.EhcacheHibernateMBean;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;

public class CacheRegionInfo {
  private final String                      regionName;
  private final String                      shortName;
  private final HibernateStatsMBeanProvider statsBeanProvider;

  private boolean                           enabled;
  private int                               tti;
  private int                               ttl;
  private int                               targetMaxInMemoryCount;
  private int                               targetMaxTotalCount;
  private boolean                           loggingEnabled;

  public static final String                ENABLED_PROP                    = "Enabled";
  public static final String                TTI_PROP                        = "TTI";
  public static final String                TTL_PROP                        = "TTL";
  public static final String                TARGET_MAX_IN_MEMORY_COUNT_PROP = "TargetMaxInMemoryCount";
  public static final String                TARGET_MAX_TOTAL_COUNT_PROP     = "TargetMaxTotalCount";
  public static final String                LOGGING_ENABLED_PROP            = "LoggingEnabled";

  public CacheRegionInfo(String regionName, Map<String, Object> attrs, HibernateStatsMBeanProvider statsBeanProvider) {
    this.regionName = regionName;
    this.shortName = CacheRegionUtils.determineShortName(regionName);
    this.statsBeanProvider = statsBeanProvider;

    setAttributes(attrs);
  }

  public void setAttributes(Map<String, Object> attrs) {
    synchronized (this) {
      this.enabled = booleanAttr(attrs, "Enabled");
      this.loggingEnabled = booleanAttr(attrs, "LoggingEnabled");
      this.tti = integerAttr(attrs, "MaxTTISeconds");
      this.ttl = integerAttr(attrs, "MaxTTLSeconds");
      this.targetMaxTotalCount = integerAttr(attrs, "TargetMaxTotalCount");
      this.targetMaxInMemoryCount = integerAttr(attrs, "TargetMaxInMemoryCount");
    }
  }

  private static Boolean booleanAttr(Map<String, Object> attrs, String name) {
    Boolean result = null;
    try {
      result = (Boolean) attrs.get(name);
    } finally {
      if (result == null) {
        result = Boolean.FALSE;
      }
    }
    return result;
  }

  private static Integer integerAttr(Map<String, Object> attrs, String name) {
    Integer result = null;
    try {
      result = (Integer) attrs.get(name);
    } finally {
      if (result == null) {
        result = Integer.valueOf(0);
      }
    }
    return result;
  }

  private EhcacheHibernateMBean getBean() {
    return statsBeanProvider.getBean();
  }

  public String getRegionName() {
    return regionName;
  }

  public String getShortName() {
    return shortName;
  }

  void _setEnabled(boolean isEnabled) {
    boolean oldEnabled = isEnabled();
    synchronized (this) {
      this.enabled = isEnabled;
    }
    firePropertyChange(ENABLED_PROP, oldEnabled, isEnabled);
  }

  public void setEnabled(boolean enabled) {
    boolean oldEnabled = isEnabled();
    if (oldEnabled != enabled) {
      synchronized (this) {
        this.enabled = enabled;
      }
      getBean().setRegionCacheEnabled(regionName, enabled);
      firePropertyChange(ENABLED_PROP, oldEnabled, enabled);
    }
  }

  public synchronized boolean isEnabled() {
    return enabled;
  }

  public void setTTI(int seconds) {
    int oldTTI = getTTI();
    if (oldTTI != seconds) {
      synchronized (this) {
        this.tti = seconds;
      }
      getBean().setRegionCacheMaxTTISeconds(regionName, seconds);
      firePropertyChange(TTI_PROP, oldTTI, seconds);
    }
  }

  public synchronized int getTTI() {
    return tti;
  }

  public void setTTL(int seconds) {
    int oldTTL = getTTL();
    if (oldTTL != seconds) {
      synchronized (this) {
        this.ttl = seconds;
      }
      getBean().setRegionCacheMaxTTLSeconds(regionName, seconds);
      firePropertyChange(TTL_PROP, oldTTL, seconds);
    }
  }

  public synchronized int getTTL() {
    return ttl;
  }

  public void setTargetMaxInMemoryCount(int targetMaxInMemoryCount) {
    int oldVal = getTargetMaxTotalCount();
    if (oldVal != targetMaxInMemoryCount) {
      synchronized (this) {
        this.targetMaxInMemoryCount = targetMaxInMemoryCount;
      }
      getBean().setRegionCacheTargetMaxInMemoryCount(regionName, targetMaxInMemoryCount);
      firePropertyChange(TARGET_MAX_IN_MEMORY_COUNT_PROP, oldVal, targetMaxInMemoryCount);
    }
  }

  public synchronized int getTargetMaxInMemoryCount() {
    return targetMaxInMemoryCount;
  }

  public void setTargetMaxTotalCount(int targetMaxTotalCount) {
    int oldVal = getTargetMaxTotalCount();
    if (oldVal != targetMaxTotalCount) {
      synchronized (this) {
        this.targetMaxTotalCount = targetMaxTotalCount;
      }
      getBean().setRegionCacheTargetMaxTotalCount(regionName, targetMaxTotalCount);
      firePropertyChange(TARGET_MAX_TOTAL_COUNT_PROP, oldVal, targetMaxTotalCount);
    }
  }

  public synchronized int getTargetMaxTotalCount() {
    return targetMaxTotalCount;
  }

  public void setLoggingEnabled(boolean enabled) {
    boolean oldLoggingEnabled = isLoggingEnabled();
    if (oldLoggingEnabled != enabled) {
      synchronized (this) {
        this.loggingEnabled = enabled;
      }
      getBean().setRegionCacheLoggingEnabled(regionName, enabled);
      firePropertyChange(LOGGING_ENABLED_PROP, oldLoggingEnabled, enabled);
    }
  }

  public synchronized boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  public void flush() {
    getBean().flushRegionCache(regionName);
  }

  public void clearStats() {
    getBean().clearStats();
  }

  // PropertyChangeSupport

  protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
      propertyChangeSupport.addPropertyChangeListener(listener);
    }
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
    }
  }

  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    if (oldValue != null || newValue != null) {
      propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
  }
}
