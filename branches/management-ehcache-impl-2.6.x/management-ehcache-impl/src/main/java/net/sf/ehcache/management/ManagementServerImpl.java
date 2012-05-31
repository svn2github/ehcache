package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.resource.services.validator.impl.EmbeddedEhcacheRequestValidator;
import net.sf.ehcache.management.service.SamplerRepositoryService;
import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryService;
import org.terracotta.management.embedded.StandaloneServer;
import org.terracotta.management.ServiceLocator;

/**
 * @author brandony
 */
public final class ManagementServerImpl implements ManagementServer {

  private final StandaloneServer standaloneServer;

  private final SamplerRepositoryService samplerRepoSvc;

  public ManagementServerImpl(ManagementRESTServiceConfiguration configuration) {
    standaloneServer = new StandaloneServer();
    setupContainer(configuration);
    loadEmbeddedAgentServiceLocator(configuration);
    SamplerRepositoryService.Locator locator = EmbeddedEhcacheServiceLocator.locator();
    this.samplerRepoSvc = locator.locateSamplerRepositoryService();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    try {
      standaloneServer.start();
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

  private void loadEmbeddedAgentServiceLocator(ManagementRESTServiceConfiguration configuration) {
    DfltSamplerRepositoryService samplerRepoSvc = new DfltSamplerRepositoryService();
    ServiceLocator.load(
        new EmbeddedEhcacheServiceLocator(true, new EmbeddedEhcacheRequestValidator(), samplerRepoSvc, samplerRepoSvc,
            samplerRepoSvc, configuration));
  }
}
