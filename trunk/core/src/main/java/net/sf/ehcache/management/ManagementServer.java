package net.sf.ehcache.management;

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;

/**
 * @author Ludovic Orban
 */
public interface ManagementServer {

  public void start();

  public void stop();

  public void setConfiguration(ManagementRESTServiceConfiguration configuration);

}
