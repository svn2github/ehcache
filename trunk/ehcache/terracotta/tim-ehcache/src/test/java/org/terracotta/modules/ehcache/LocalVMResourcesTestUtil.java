/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import java.util.concurrent.ConcurrentMap;

public class LocalVMResourcesTestUtil {

  public static ConcurrentMap<String, Object> getRegisteredResources() {
    return LocalVMResources.getInstance().getRegisteredResources();
  }
}
