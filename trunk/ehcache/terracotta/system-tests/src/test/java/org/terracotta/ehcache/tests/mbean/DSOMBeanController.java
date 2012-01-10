/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.mbean;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

/**
 * @author Abhishek Sanoujam
 */
public class DSOMBeanController implements DSOMBean {

  private static final ObjectName DSO;
  private static final ObjectName DUMPER;
  static {
    ObjectName dso;
    ObjectName dumper = null;
    try {
      dso = new ObjectName("org.terracotta:type=Terracotta Server,name=DSO");
      dumper = new ObjectName("org.terracotta.internal:type=Terracotta Server,name=L2Dumper");
    } catch (Exception e) {
      dso = null;
      dumper = null;
    }
    DSO = dso;
    DUMPER = dumper;
  }

  private final String            host;
  private final int               jmxPort;

  public DSOMBeanController(String host, int jmxPort) {
    this.host = host;
    this.jmxPort = jmxPort;
  }

  private <T> T performL2MBeanDumper(DSOMBeanAction<T> action) throws IOException {
    return performL2ControlBeanAction(action, DUMPER);
  }

  private <T> T performL2MBeanTCServerInfo(DSOMBeanAction<T> action) throws IOException {
    return performL2ControlBeanAction(action, DSO);
  }

  private <T> T performL2ControlBeanAction(DSOMBeanAction<T> action, ObjectName objectName) throws IOException {
    if (objectName == null) { throw new RuntimeException(objectName + " object name is null"); }
    final JMXConnector jmxConnector = JMXUtils.getJMXConnector(host, jmxPort);
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    DSOMBean l2ControlBean = DSOMBeanProxy.newL2ControlMBeanProxy(mbs, objectName);
    try {
      return action.performL2ControlBeanAction(l2ControlBean);
    } finally {
      try {
        jmxConnector.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public long getGlobalServerMapGetSizeRequestsCount() {

    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Long>() {

        public Long performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getGlobalServerMapGetSizeRequestsCount();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public long getGlobalServerMapGetSizeRequestsRate() {
    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Long>() {

        public Long performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getGlobalServerMapGetSizeRequestsRate();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public long getGlobalServerMapGetValueRequestsCount() {
    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Long>() {

        public Long performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getGlobalServerMapGetValueRequestsCount();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public long getGlobalServerMapGetValueRequestsRate() {
    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Long>() {

        public Long performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getGlobalServerMapGetValueRequestsRate();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public long getObjectFaultRate() {
    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Long>() {

        public Long performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getObjectFaultRate();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<ObjectName, Long> getServerMapGetSizeRequestsCount() {
    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Map<ObjectName, Long>>() {

        public Map<ObjectName, Long> performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getServerMapGetSizeRequestsCount();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<ObjectName, Long> getServerMapGetSizeRequestsRate() {
    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Map<ObjectName, Long>>() {

        public Map<ObjectName, Long> performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getServerMapGetSizeRequestsRate();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<ObjectName, Long> getServerMapGetValueRequestsCount() {
    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Map<ObjectName, Long>>() {

        public Map<ObjectName, Long> performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getServerMapGetValueRequestsCount();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<ObjectName, Long> getServerMapGetValueRequestsRate() {
    try {
      return performL2MBeanTCServerInfo(new DSOMBeanAction<Map<ObjectName, Long>>() {

        public Map<ObjectName, Long> performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.getServerMapGetValueRequestsRate();
        }

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static interface DSOMBeanAction<T> {
    T performL2ControlBeanAction(DSOMBean l2ControlBean);
  }

  public Void dumpClusterState() {
    try {
      return performL2MBeanDumper(new DSOMBeanAction<Void>() {

        public Void performL2ControlBeanAction(DSOMBean dsoMBean) {
          return dsoMBean.dumpClusterState();
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
