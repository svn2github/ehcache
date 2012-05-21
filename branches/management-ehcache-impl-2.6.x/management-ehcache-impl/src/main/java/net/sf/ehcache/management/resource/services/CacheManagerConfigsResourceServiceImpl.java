package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.resource.ConfigContainerEntity;
import net.sf.ehcache.management.service.EmbeddedEhcacheServiceLocator;
import net.sf.ehcache.management.service.EntityResourceFactory;
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
@Path("/agents/cacheManagers/configs")
public final class CacheManagerConfigsResourceServiceImpl implements CacheManagerConfigsResourceService {
  private final EntityResourceFactory entityResourceFactory;

  private final RequestValidator validator;

  public CacheManagerConfigsResourceServiceImpl() {
    this.entityResourceFactory = EmbeddedEhcacheServiceLocator.locator().locateEntityResourceFactory();
    this.validator = EmbeddedEhcacheServiceLocator.locator().locateRequestValidator();
  }

  @Override
  public Response getXMLCacheManagerConfigs(UriInfo info) {
    validator.validateSafe(info);

    String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

    Collection<CacheManagerConfigEntity> configs = entityResourceFactory.createCacheManagerConfigEntities(cmNames);

    ConfigContainerEntity<CacheManagerConfigEntity> cc = new ConfigContainerEntity<CacheManagerConfigEntity>();
    cc.setConfiguration(configs);
    cc.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);

    return Response.ok(cc).build();
  }
}
