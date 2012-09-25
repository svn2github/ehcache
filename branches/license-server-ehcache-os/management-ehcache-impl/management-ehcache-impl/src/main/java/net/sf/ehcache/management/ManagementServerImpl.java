/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.resource.services.validator.impl.EmbeddedEhcacheRequestValidator;
import net.sf.ehcache.management.service.AgentService;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import net.sf.ehcache.management.service.SamplerRepositoryService;
import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.embedded.StandaloneServer;
import org.terracotta.management.resource.services.LicenseService;
import org.terracotta.management.resource.services.LicenseServiceImpl;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author brandony
 */
public final class ManagementServerImpl implements ManagementServer {

  private static final Logger LOG = LoggerFactory.getLogger(ManagementServerImpl.class);

  private final StandaloneServer standaloneServer;

  private volatile DfltSamplerRepositoryService samplerRepoSvc;

  public ManagementServerImpl(String clientUUID, ManagementRESTServiceConfiguration configuration) {

    // Clear settings that are invalid for non-ee management servers
    configuration.setNeedClientAuth(false);
    configuration.setSecurityServiceLocation(null);
    configuration.setSslEnabled(false);
    configuration.setSecurityServiceTimeout(0);

    String basePackage = "net.sf.ehcache.management";
    String host = configuration.getHost();
    int port = configuration.getPort();

    loadEmbeddedAgentServiceLocator(clientUUID, configuration);
    standaloneServer = new StandaloneServer(null, null, basePackage, host, port, null, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    try {
      standaloneServer.start();
      this.samplerRepoSvc = (DfltSamplerRepositoryService) ServiceLocator.locate(SamplerRepositoryService.class);
    } catch (Exception e) {
      throw new CacheException("error starting management server", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    try {
      samplerRepoSvc.dispose();
      standaloneServer.stop();
    } catch (Exception e) {
      throw new CacheException("error stopping management server", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(CacheManager managedResource) {
    samplerRepoSvc.register(managedResource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(CacheManager managedResource) {
    samplerRepoSvc.unregister(managedResource);
    ServiceLocator.unload();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasRegistered() {
    return samplerRepoSvc.hasRegistered();
  }

  private void loadEmbeddedAgentServiceLocator(String clientUUID, ManagementRESTServiceConfiguration configuration) {
    DfltSamplerRepositoryService samplerRepoSvc = new DfltSamplerRepositoryService(clientUUID, configuration);
    LicenseService licenseService = new LicenseServiceImpl(false);

    ServiceLocator locator = new ServiceLocator()
                                    .loadService(LicenseService.class, licenseService)
                                    .loadService(RequestValidator.class, new EmbeddedEhcacheRequestValidator())
                                    .loadService(CacheManagerService.class, samplerRepoSvc)
                                    .loadService(CacheService.class, samplerRepoSvc)
                                    .loadService(EntityResourceFactory.class, samplerRepoSvc)
                                    .loadService(SamplerRepositoryService.class, samplerRepoSvc)
                                    .loadService(AgentService.class, samplerRepoSvc)
                                    .loadService(ManagementRESTServiceConfiguration.class, configuration);

    ServiceLocator.load(locator);
  }
}
