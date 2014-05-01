/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import javax.ws.rs.core.Application;

public class ApplicationEhCache extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>();
    ServiceLoader<ApplicationEhCacheService> loader = ServiceLoader.load(ApplicationEhCacheService.class);
    for (ApplicationEhCacheService applicationEhCacheService : loader) {
      s.addAll(applicationEhCacheService.getClasses());
    }
    return s;
  }

}