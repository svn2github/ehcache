package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.resource.services.validator.impl.EmbeddedEhcacheRequestValidator;
import net.sf.ehcache.management.service.EmbeddedEhcacheServiceLocator;
import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryService;
import org.terracotta.management.embedded.StandaloneServer;
import org.terracotta.management.service.ServiceLocator;

/**
 * @author brandony
 */
public class ManagementServerImpl implements ManagementServer {

  private final StandaloneServer standaloneServer;

  public ManagementServerImpl(ManagementRESTServiceConfiguration configuration) {
    DfltSamplerRepositoryService samplerRepoSvc = new DfltSamplerRepositoryService();
    ServiceLocator.load(
        new EmbeddedEhcacheServiceLocator(new EmbeddedEhcacheRequestValidator(), samplerRepoSvc, samplerRepoSvc,
            samplerRepoSvc));

    standaloneServer = new StandaloneServer();
    standaloneServer.setBasePackage("net.sf.ehcache.management");
    standaloneServer.setHost(configuration.getHost());
    standaloneServer.setPort(configuration.getPort());
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
    EmbeddedEhcacheServiceLocator.locator().locateSamplerRepositoryService().register(managedResource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(CacheManager managedResource) {
    EmbeddedEhcacheServiceLocator.locator().locateSamplerRepositoryService().unregister(managedResource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasRegistered() {
    return EmbeddedEhcacheServiceLocator.locator().locateSamplerRepositoryService().hasRegistered();
  }
}
