package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.services.SamplerRepositoryService;
import net.sf.ehcache.management.services.impl.DfltSamplerRepositoryService;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.terracotta.management.embedded.StandaloneServer;

/**
 * @author Ludovic Orban
 * @author brandony
 */
public class ManagementServerImpl implements ManagementServer {

  private final StandaloneServer standaloneServer = new StandaloneServer();

  private SamplerRepositoryService samplerRepoSvc;

  public ManagementServerImpl() {
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
  public void setConfiguration(ManagementRESTServiceConfiguration configuration) {
    standaloneServer.setBasePackage("net.sf.ehcache.management");
    standaloneServer.setHost(configuration.getHost());
    standaloneServer.setPort(configuration.getPort());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(CacheManager managedResource) {
    if(samplerRepoSvc == null) {
      ApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
      samplerRepoSvc =  ctx.getBean(DfltSamplerRepositoryService.class);
    }
    samplerRepoSvc.register(managedResource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(CacheManager managedResource) {
    if(samplerRepoSvc != null) samplerRepoSvc.unregister(managedResource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasRegistered() {
    return samplerRepoSvc != null && samplerRepoSvc.hasRegistered();
  }
}
