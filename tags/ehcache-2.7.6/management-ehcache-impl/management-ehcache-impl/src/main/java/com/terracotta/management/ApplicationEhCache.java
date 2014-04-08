package com.terracotta.management;

import java.util.HashSet;
import java.util.Set;

public class ApplicationEhCache extends javax.ws.rs.core.Application {


  public Set<Class<?>> getClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>();
    s.add(net.sf.ehcache.management.resource.services.ElementsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheStatisticSamplesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CachesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagersResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagerConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.AgentsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.exceptions.WebApplicationExceptionMapper.class);
    s.add(net.sf.ehcache.management.resource.exceptions.DefaultExceptionMapper.class);
    s.add(net.sf.ehcache.management.resource.exceptions.ResourceRuntimeExceptionMapper.class);
    return s;
  }


}