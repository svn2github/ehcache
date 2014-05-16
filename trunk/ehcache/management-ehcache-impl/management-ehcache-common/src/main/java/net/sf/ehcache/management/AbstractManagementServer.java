package net.sf.ehcache.management;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.service.ManagementServerLifecycle;

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

  protected final List<ManagementServerLifecycle> managementServerLifecycles = new CopyOnWriteArrayList<ManagementServerLifecycle>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    try {
      standaloneServer.start();
    } catch (Exception e) {
      for (ManagementServerLifecycle samplerRepoService : managementServerLifecycles) {
        samplerRepoService.dispose();
      }
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
      for (ManagementServerLifecycle samplerRepoService : managementServerLifecycles) {
        samplerRepoService.dispose();
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
    for (ManagementServerLifecycle samplerRepoService : managementServerLifecycles) {
      samplerRepoService.register(managedResource);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(CacheManager managedResource) {
    for (ManagementServerLifecycle samplerRepoService : managementServerLifecycles) {
      samplerRepoService.unregister(managedResource);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasRegistered() {
    boolean hasRegistered = true;
    for (ManagementServerLifecycle samplerRepoService : managementServerLifecycles) {
      hasRegistered = hasRegistered && samplerRepoService.hasRegistered();
    }
    return hasRegistered;
  }
}
