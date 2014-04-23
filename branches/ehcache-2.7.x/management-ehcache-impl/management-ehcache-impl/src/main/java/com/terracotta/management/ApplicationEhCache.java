package com.terracotta.management;

import java.util.HashSet;
import java.util.Set;

import org.terracotta.management.application.DefaultApplication;

public class ApplicationEhCache extends DefaultApplication {

  @Override
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