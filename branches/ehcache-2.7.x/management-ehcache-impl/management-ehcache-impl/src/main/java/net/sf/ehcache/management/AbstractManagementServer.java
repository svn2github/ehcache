package net.sf.ehcache.management;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.service.SamplerRepositoryService;

import org.terracotta.management.ServiceLocator;
import org.terracotta.management.embedded.StandaloneServer;

/**
 * 
 * ManagementServerImpl comes in 2 flavors : open source and ee
 * This class defines common behavior between those two.
 * 
 * @author Anthony Dahanne
 *
 */
public abstract class AbstractManagementServer implements ManagementServer {

  protected StandaloneServer standaloneServer;

  protected volatile SamplerRepositoryService samplerRepoSvc;

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    try {
      standaloneServer.start();
    } catch (Exception e) {
      samplerRepoSvc.dispose();
      ServiceLocator.unload();
      throw new CacheException("error starting management server", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    try {
      if (samplerRepoSvc.hasRegistered()) {
        samplerRepoSvc.dispose();
      }
      standaloneServer.stop();
      ServiceLocator.unload();
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

}
