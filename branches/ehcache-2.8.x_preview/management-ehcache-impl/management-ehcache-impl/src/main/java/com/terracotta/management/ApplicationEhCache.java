/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management;

import org.terracotta.management.application.DefaultApplication;

import java.util.HashSet;
import java.util.Set;

public class ApplicationEhCache extends DefaultApplication {

  public Set<Class<?>> getClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>(super.getClasses());
    s.add(net.sf.ehcache.management.resource.services.ElementsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheStatisticSamplesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CachesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagersResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagerConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.AgentsResourceServiceImpl.class);
    return s;
  }

}