/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.resource.services.validator.impl.EmbeddedEhcacheRequestValidator;
import net.sf.ehcache.management.service.SamplerRepositoryService;
import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.embedded.StandaloneServer;

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
    standaloneServer = new StandaloneServer();
    setupContainer(configuration);
    loadEmbeddedAgentServiceLocator(clientUUID, configuration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    try {
      standaloneServer.start();
      SamplerRepositoryService.Locator locator = EmbeddedEhcacheServiceLocator.locator();
      this.samplerRepoSvc = (DfltSamplerRepositoryService)locator.locateSamplerRepositoryService();
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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasRegistered() {
    return samplerRepoSvc.hasRegistered();
  }

  private void setupContainer(ManagementRESTServiceConfiguration configuration) {
    standaloneServer.setBasePackage("net.sf.ehcache.management");
    standaloneServer.setHost(configuration.getHost());
    standaloneServer.setPort(configuration.getPort());
  }

  private void loadEmbeddedAgentServiceLocator(String clientUUID, ManagementRESTServiceConfiguration configuration) {
    //Clear settings that are invalid for non-ee management servers
    configuration.setNeedClientAuth(false);
    configuration.setSecurityServiceLocation(null);
    configuration.setSslEnabled(false);
    configuration.setSecurityServiceTimeout(0);

    ObjectName objectName = null;
    try {
      objectName = new ObjectName("net.sf.ehcache:type=RepositoryService,bind=" + configuration.getBind()
          .replace(":", "_") + ",node=" + clientUUID);
    } catch (MalformedObjectNameException me) {
      LOG.warn("Error creating MBean name", me);
    }

    DfltSamplerRepositoryService samplerRepoSvc = new DfltSamplerRepositoryService(objectName, configuration);
    ServiceLocator.load(
        new EmbeddedEhcacheServiceLocator(true, new EmbeddedEhcacheRequestValidator(), samplerRepoSvc, samplerRepoSvc,
            samplerRepoSvc, samplerRepoSvc, samplerRepoSvc));
  }
}
