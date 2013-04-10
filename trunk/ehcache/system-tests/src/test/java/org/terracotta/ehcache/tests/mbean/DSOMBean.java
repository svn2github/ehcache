/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.mbean;

import java.util.Map;

import javax.management.ObjectName;

/**
 * @author Abhishek Sanoujam
 */
public interface DSOMBean {

  long getGlobalServerMapGetSizeRequestsCount();

  long getGlobalServerMapGetValueRequestsCount();

  long getGlobalServerMapGetSizeRequestsRate();

  long getGlobalServerMapGetValueRequestsRate();

  long getReadOperationRate();

  Map<ObjectName, Long> getServerMapGetSizeRequestsCount();

  Map<ObjectName, Long> getServerMapGetValueRequestsCount();

  Map<ObjectName, Long> getServerMapGetSizeRequestsRate();

  Map<ObjectName, Long> getServerMapGetValueRequestsRate();

  Void dumpClusterState();
}
