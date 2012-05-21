package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.jmx.CacheJmxClient;
import net.sf.ehcache.management.resource.CacheEntity;
import org.terracotta.management.resource.AgentEntity;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
// TODO: Give the correct resource location some thought when we start working on the TSA client impl of these. Maybe there
// is shared hierarchy? Maybe we have common code to factor out? For now this class and resource path will serve as a placeholder.
@Path("/agents/tsa/cacheManagers/caches")
public class ClusteredCachesResourceServiceImpl implements CachesResourceService {

  private static final List<String> EXCLUDED_ATTRIBUTES = Arrays.asList("CacheName");

  /**
   * @see net.sf.ehcache.management.resource.services.CachesResourceService#getCaches(javax.ws.rs.core.UriInfo)
   */
  @Override
  public Collection<CacheEntity> getCaches(final UriInfo info) {
    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");
    if (ids != null && !AgentEntity.EMBEDDED_AGENT_ID.equals(ids)) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(String.format("Agent ID must be '%s'.", AgentEntity.EMBEDDED_AGENT_ID)).build());
    }

    String cacheManagerNamesString = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    List<String> cacheManagerNames;
    if (cacheManagerNamesString == null) {
      cacheManagerNames = null;
    } else {
      String[] names = cacheManagerNamesString.split("\\,");
      cacheManagerNames = Arrays.asList(names);
    }

    try {
      List<CacheEntity> result = new ArrayList<CacheEntity>();

      MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
      Map<String, Map<String, ObjectName>> allCacheObjectNamesByCacheNameByCacheManagerName = CacheJmxClient
          .getAllCacheObjectNamesByCacheNameByCacheManagerName(platformMBeanServer, cacheManagerNames);

      String cacheNamesString = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
      MultivaluedMap<String, String> qParams = info.getQueryParameters();

      if (cacheNamesString == null) {
        // get all caches from specified cache managers
        for (Map.Entry<String, Map<String, ObjectName>> cacheManagerEntry : allCacheObjectNamesByCacheNameByCacheManagerName
            .entrySet()) {
          for (Map.Entry<String, ObjectName> cacheEntry : cacheManagerEntry.getValue().entrySet()) {
            result.add(buildEntity(platformMBeanServer, cacheManagerEntry.getKey(), cacheEntry.getValue(), qParams));
          }
        }
      } else {
        // get specified caches from specified cache managers, at least one cache manager must have a cache with such name
        // or we throw an exception stating that no cache with such name is known
        String[] cacheNames = cacheNamesString.split("\\,");
        for (String cacheName : cacheNames) {
          boolean found = false;
          for (Map.Entry<String, Map<String, ObjectName>> cacheManagerEntry : allCacheObjectNamesByCacheNameByCacheManagerName
              .entrySet()) {
            ObjectName objectName = cacheManagerEntry.getValue().get(cacheName);
            if (objectName != null) {
              found = true;
              String cacheManagerName = cacheManagerEntry.getKey();
              result.add(buildEntity(platformMBeanServer, cacheManagerName, objectName, qParams));
            }
          }

          if (!found) {
            throw new IllegalArgumentException("unknown cache with name [" + cacheName + "]");
          }
        }
      }

      return result;
    } catch (Exception ex) {
      throw new WebApplicationException(
          Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
    }
  }

  private CacheEntity buildEntity(MBeanServer platformMBeanServer,
                                  String cacheManagerName,
                                  ObjectName objectName,
                                  MultivaluedMap<String, String> qParams)
      throws InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException,
      AttributeNotFoundException {
    CacheEntity e = new CacheEntity();
    e.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    e.setCacheManagerName(cacheManagerName);

    String cacheName = (String) platformMBeanServer.getAttribute(objectName, CacheJmxClient.CACHE_JMX_NAME_ATTRIBUTE);
    e.setName(cacheName);

    MBeanAttributeInfo[] attributes = platformMBeanServer.getMBeanInfo(objectName).getAttributes();
    for (MBeanAttributeInfo attribute : attributes) {
      String attrName = attribute.getName();
      if (!EXCLUDED_ATTRIBUTES.contains(attrName) && (qParams == null || qParams.get(ATTR_QUERY_KEY) == null || qParams
          .get(ATTR_QUERY_KEY).contains(attrName))) {
        Object attrValue = platformMBeanServer.getAttribute(objectName, attrName);
        e.getAttributes().put(attrName, attrValue);
      }
    }
    return e;
  }

  /**
   * @see net.sf.ehcache.management.resource.services.CachesResourceService#createCache(javax.ws.rs.core.UriInfo)
   */
  @Override
  public void createOrUpdateCache(final UriInfo info,
                                  CacheEntity resource) {
    //todo: implement
    throw new WebApplicationException(
        Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Not yet implemented").build());
  }

  /**
   * @see net.sf.ehcache.management.resource.services.CachesResourceService#deleteCache(javax.ws.rs.core.UriInfo)
   */
  @Override
  public void deleteCache(final UriInfo info) {
    //todo: implement
    throw new WebApplicationException(
        Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Not yet implemented").build());
  }

  @Override
  public void wipeStatistics(@Context UriInfo info) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
