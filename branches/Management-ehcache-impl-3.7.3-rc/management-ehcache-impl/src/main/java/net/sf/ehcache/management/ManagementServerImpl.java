package net.sf.ehcache.management;

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.resource.services.validator.impl.EmbeddedEhcacheRequestValidator;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import net.sf.ehcache.management.service.SamplerRepositoryService;
import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryService;

import org.terracotta.management.ServiceLocator;
import org.terracotta.management.embedded.StandaloneServer;
import org.terracotta.management.resource.services.LicenseService;
import org.terracotta.management.resource.services.LicenseServiceImpl;
import org.terracotta.management.resource.services.validator.RequestValidator;

/**
 * @author brandony
 */
public final class ManagementServerImpl extends AbstractManagementServer {

  public ManagementServerImpl(ManagementRESTServiceConfiguration configuration) {

    // Clear settings that are invalid for non-ee management servers
    configuration.setNeedClientAuth(false);
    configuration.setSecurityServiceLocation(null);
    configuration.setSslEnabled(false);
    configuration.setSecurityServiceTimeout(0);

    String basePackage = "net.sf.ehcache.management";
    String host = configuration.getHost();
    int port = configuration.getPort();

    loadEmbeddedAgentServiceLocator(configuration);
    this.samplerRepoSvc = ServiceLocator.locate(SamplerRepositoryService.class);
    samplerRepoSvc = ServiceLocator.locate(SamplerRepositoryService.class);
    standaloneServer = new StandaloneServer(null, null, basePackage, host, port, null, false);
  }

  private void loadEmbeddedAgentServiceLocator(ManagementRESTServiceConfiguration configuration) {

    DfltSamplerRepositoryService samplerRepoSvc = new DfltSamplerRepositoryService();
    LicenseService licenseService = new LicenseServiceImpl(false);

    ServiceLocator locator = new ServiceLocator()
                                    .loadService(LicenseService.class, licenseService)
                                    .loadService(RequestValidator.class, new EmbeddedEhcacheRequestValidator())
                                    .loadService(CacheManagerService.class, samplerRepoSvc)
                                    .loadService(CacheService.class, samplerRepoSvc)
                                    .loadService(EntityResourceFactory.class, samplerRepoSvc)
                                    .loadService(SamplerRepositoryService.class, samplerRepoSvc)
                                    .loadService(ManagementRESTServiceConfiguration.class, configuration);

    ServiceLocator.load(locator);

  }
}
