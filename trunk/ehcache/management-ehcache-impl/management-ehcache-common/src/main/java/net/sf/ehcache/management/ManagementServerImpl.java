/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management;

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.service.ManagementServerLifecycle;
import net.sf.ehcache.management.service.impl.RemoteAgentEndpointImpl;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.embedded.FilterDetail;
import org.terracotta.management.embedded.NoIaFilter;
import org.terracotta.management.embedded.StandaloneServer;
import org.terracotta.management.resource.services.LicenseService;
import org.terracotta.management.resource.services.LicenseServiceImpl;

import com.terracotta.management.ApplicationEhCacheService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;

/**
 * @author brandony
 */
public final class ManagementServerImpl extends AbstractManagementServer {

  private RemoteAgentEndpointImpl remoteAgentEndpointImpl;

  @Override
  public void initialize(ManagementRESTServiceConfiguration configuration) {

    // Clear settings that are invalid for non-ee management servers
    configuration.setNeedClientAuth(false);
    configuration.setSecurityServiceLocation(null);
    configuration.setSslEnabled(false);
    configuration.setSecurityServiceTimeout(0);

    String host = configuration.getHost();
    int port = configuration.getPort();

    loadEmbeddedAgentServiceLocator(configuration);

    ServiceLoader<ApplicationEhCacheService> loaders = applicationEhCacheServiceLoader();
    for (ApplicationEhCacheService applicationEhCacheService : loaders) {
      Class<ManagementServerLifecycle> clazz = applicationEhCacheService.getManagementServerLifecyle();
      managementServerLifecycles.add(ServiceLocator.locate(clazz));
    }

    List<FilterDetail> filterDetails = Collections.singletonList(new FilterDetail(new NoIaFilter(), "/*"));
    standaloneServer = new StandaloneServer(filterDetails, null, "com.terracotta.management.ApplicationEhCache",
        host, port, null, false);
  }

  @Override
  public void registerClusterRemoteEndpoint(String clientUUID) {
    remoteAgentEndpointImpl.registerMBean(clientUUID);
  }

  @Override
  public void unregisterClusterRemoteEndpoint(String clientUUID) {
    remoteAgentEndpointImpl.unregisterMBean(clientUUID);
  }

  private <T> void loadEmbeddedAgentServiceLocator(ManagementRESTServiceConfiguration configuration) {
    // TODO : refactor loop through service loaders
    remoteAgentEndpointImpl = new RemoteAgentEndpointImpl();

    ServiceLocator locator = new ServiceLocator();
    LicenseService licenseService = new LicenseServiceImpl(false);
    ServiceLoader<ApplicationEhCacheService> loader = applicationEhCacheServiceLoader();
    for (ApplicationEhCacheService applicationEhCacheService : loader) {
      Map<Class<T>, T> serviceClasses = applicationEhCacheService.getServiceClasses(configuration, remoteAgentEndpointImpl);
      for (Entry<Class<T>, T> entry : serviceClasses.entrySet()) {
        locator.loadService(entry.getKey(), entry.getValue());
      }
    }
    locator.loadService(LicenseService.class, licenseService);
    locator.loadService(ManagementRESTServiceConfiguration.class, configuration);

    ServiceLocator.load(locator);
  }
}
