package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.EmbeddedEhcacheServiceLocator;
import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.resource.ConfigContainerEntity;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author brandony
 */
@Path("/agents/cacheManagers/caches/configs")
public final class CacheConfigsResourceServiceImpl implements CacheConfigsResourceService {
  private static final Logger LOG = LoggerFactory.getLogger(CacheConfigsResourceServiceImpl.class);

  private final EntityResourceFactory entityResourceFactory;

  private final RequestValidator validator;

  public CacheConfigsResourceServiceImpl() {
    EntityResourceFactory.Locator entityRsrcFactoryLocator = EmbeddedEhcacheServiceLocator.locator();
    this.entityResourceFactory = entityRsrcFactoryLocator.locateEntityResourceFactory();
    RequestValidator.Locator reqValidatorLocator = EmbeddedEhcacheServiceLocator.locator();
    this.validator = reqValidatorLocator.locateRequestValidator();
  }

  @Override
  public Response getXMLCacheConfigs(UriInfo info) {
    LOG.info(String.format("Invoking CacheConfigsResourceServiceImpl.getXMLCacheConfigs: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    Collection<CacheConfigEntity> configs = null;

      configs = entityResourceFactory.createCacheConfigEntities(cmNames, cNames);

    ConfigContainerEntity<CacheConfigEntity> cc = new ConfigContainerEntity<CacheConfigEntity>();
    cc.setConfiguration(configs);
    cc.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);

    return Response.ok(cc).build();
  }
}
