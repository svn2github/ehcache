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
import net.sf.ehcache.management.resource.services.validator.impl.EmbeddedEhcacheRequestValidatorV2;
import net.sf.ehcache.management.service.CacheManagerServiceV2;
import net.sf.ehcache.management.service.CacheServiceV2;
import net.sf.ehcache.management.service.EntityResourceFactoryV2;
import net.sf.ehcache.management.service.ManagementServerLifecycle;
import net.sf.ehcache.management.service.SamplerRepositoryServiceV2;
import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryServiceV2;
import net.sf.ehcache.management.service.impl.RemoteAgentEndpointImpl;

import org.terracotta.management.application.DefaultApplicationV2;
import org.terracotta.management.resource.services.AgentServiceV2;
import org.terracotta.management.resource.services.events.EventServiceV2;
import org.terracotta.management.resource.services.validator.RequestValidator;

public class ApplicationEhCacheV2 extends DefaultApplicationV2 implements ApplicationEhCacheService {

  @Override
  public Set<Class<?>> getRestResourceClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>(super.getClasses());
    s.add(net.sf.ehcache.management.resource.services.ElementsResourceServiceImplV2.class);
    s.add(net.sf.ehcache.management.resource.services.CacheStatisticSamplesResourceServiceImplV2.class);
    s.add(net.sf.ehcache.management.resource.services.CachesResourceServiceImplV2.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagersResourceServiceImplV2.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagerConfigsResourceServiceImplV2.class);
    s.add(net.sf.ehcache.management.resource.services.CacheConfigsResourceServiceImplV2.class);
    s.add(net.sf.ehcache.management.resource.services.QueryResourceServiceImplV2.class);
    return s;
  }

  @Override
  public Map<Class<?>, Object> getServiceClasses(String clientUUID, ManagementRESTServiceConfiguration configuration, RemoteAgentEndpointImpl agentEndpointImpl) {
    DfltSamplerRepositoryServiceV2 samplerRepoSvc = new DfltSamplerRepositoryServiceV2(clientUUID, configuration,
        agentEndpointImpl);
    Map<Class<?>, Object> serviceClasses = new HashMap<Class<?>, Object>();
    serviceClasses.put(RequestValidator.class, new EmbeddedEhcacheRequestValidatorV2());
    serviceClasses.put(CacheManagerServiceV2.class, samplerRepoSvc);
    serviceClasses.put(CacheServiceV2.class, samplerRepoSvc);
    serviceClasses.put(EntityResourceFactoryV2.class, samplerRepoSvc);
    serviceClasses.put(SamplerRepositoryServiceV2.class, samplerRepoSvc);
    serviceClasses.put(AgentServiceV2.class, samplerRepoSvc);
    serviceClasses.put(EventServiceV2.class, samplerRepoSvc);
    return serviceClasses;
  }

  @Override
  public Class<? extends ManagementServerLifecycle> getManagementServerLifecyle() {
    return SamplerRepositoryServiceV2.class;
  }

}