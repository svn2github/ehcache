package net.sf.ehcache.management;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class DummyManagementServerImpl implements ManagementServer {

  public static enum Status {
    STARTED, STOPPED
  }

  public static Status status = Status.STOPPED;
  public final static Map<String, CacheManager> registeredCacheManagers = new HashMap<String, CacheManager>();

  public DummyManagementServerImpl() {
  }

  @Override
  public void start() {
    status = Status.STARTED;
  }

  @Override
  public void stop() {
    status = Status.STOPPED;
  }

  @Override
  public void register(CacheManager managedResource) {
    registeredCacheManagers.put(managedResource.getName(), managedResource);
  }

  @Override
  public void unregister(CacheManager managedResource) {
    registeredCacheManagers.remove(managedResource.getName());
  }

  @Override
  public boolean hasRegistered() {
    return !registeredCacheManagers.isEmpty();
  }

  @Override
  public void initialize(String clientUUID, ManagementRESTServiceConfiguration configuration) {
  }
}
