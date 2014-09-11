/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.resource.services.validator.impl.EmbeddedEhcacheRequestValidator;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import net.sf.ehcache.management.service.ManagementServerLifecycle;
import net.sf.ehcache.management.service.SamplerRepositoryService;
import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryService;
import net.sf.ehcache.management.service.impl.RemoteAgentEndpointImpl;

import org.terracotta.management.application.DefaultApplication;
import org.terracotta.management.resource.services.AgentService;
import org.terracotta.management.resource.services.validator.RequestValidator;

public class ApplicationEhCacheV1 extends DefaultApplication implements ApplicationEhCacheService {

  @Override
  public Set<Class<?>> getRestResourceClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>(super.getClasses());
    s.add(net.sf.ehcache.management.resource.services.ElementsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheStatisticSamplesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CachesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagersResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagerConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.QueryResourceServiceImpl.class);
    return s;
  }

  @Override
  public Map<Class<?>, Object> getServiceClasses(ManagementRESTServiceConfiguration configuration, RemoteAgentEndpointImpl agentEndpointImpl) {
    DfltSamplerRepositoryService samplerRepoSvc = new DfltSamplerRepositoryService(configuration, agentEndpointImpl);
    Map<Class<?>, Object> serviceClasses = new HashMap<Class<?>, Object>();
    serviceClasses.put(RequestValidator.class, new EmbeddedEhcacheRequestValidator());
    serviceClasses.put(CacheManagerService.class, samplerRepoSvc);
    serviceClasses.put(CacheService.class, samplerRepoSvc);
    serviceClasses.put(EntityResourceFactory.class, samplerRepoSvc);
    serviceClasses.put(SamplerRepositoryService.class, samplerRepoSvc);
    serviceClasses.put(AgentService.class, samplerRepoSvc);
    return serviceClasses;
  }

  @Override
  public Class<? extends ManagementServerLifecycle> getManagementServerLifecyle() {
    return SamplerRepositoryService.class;
  }

}