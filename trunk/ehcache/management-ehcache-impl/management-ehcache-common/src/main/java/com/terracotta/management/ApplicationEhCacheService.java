package com.terracotta.management;

import java.util.Map;
import java.util.Set;

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.service.ManagementServerLifecycle;
import net.sf.ehcache.management.service.impl.RemoteAgentEndpointImpl;

public interface ApplicationEhCacheService<T> {
  public Set<Class<?>> getRestResourceClasses();

  Map<Class<?>, Object> getServiceClasses(String clientUUID, ManagementRESTServiceConfiguration configuration,
      RemoteAgentEndpointImpl remoteAgentEndpointImpl);

  Class<? extends ManagementServerLifecycle> getManagementServerLifecyle();
}
