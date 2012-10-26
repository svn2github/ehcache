package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.jmx.CacheManagerJmxClient;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import org.terracotta.management.resource.AgentEntity;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
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
@Path("/agents/tsa/cacheManagers")
public class ClusteredCacheManagersResourceServiceImpl implements CacheManagersResourceService {

  private static final List<String> EXCLUDED_ATTRIBUTES = Arrays.asList("MBeanRegisteredName", "Name");

  /**
   * @see net.sf.ehcache.management.resource.services.CacheManagersResourceService#getCacheManagers(javax.ws.rs.core.UriInfo)
   */
  public Collection<CacheManagerEntity> getCacheManagers(UriInfo info) {
    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");
    if (ids != null && !AgentEntity.EMBEDDED_AGENT_ID.equals(ids)) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(String.format("Agent ID must be '%s'.", AgentEntity.EMBEDDED_AGENT_ID)).build());
    }

    try {
      String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
      MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
      List<ObjectName> cacheManagerObjectNames = findCacheManagerObjectNames(platformMBeanServer,
          names == null ? null : names.split(","));

      List<CacheManagerEntity> result = new ArrayList<CacheManagerEntity>();
      for (ObjectName cacheManagerObjectName : cacheManagerObjectNames) {
        CacheManagerEntity e = new CacheManagerEntity();
        e.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
        String cacheManagerName = (String) platformMBeanServer
            .getAttribute(cacheManagerObjectName, CacheManagerJmxClient.CACHE_MANAGER_JMX_NAME_ATTRIBUTE);
        e.setName(cacheManagerName);

        MultivaluedMap<String, String> qParams = info.getQueryParameters();
        MBeanAttributeInfo[] attributes = platformMBeanServer.getMBeanInfo(cacheManagerObjectName).getAttributes();
        for (MBeanAttributeInfo attribute : attributes) {
          String attrName = attribute.getName();

          if (!EXCLUDED_ATTRIBUTES.contains(attrName) && (qParams == null || qParams
              .get(ATTR_QUERY_KEY) == null || qParams.get(ATTR_QUERY_KEY).contains(attrName))) {
            Object attrValue = platformMBeanServer.getAttribute(cacheManagerObjectName, attrName);
            e.getAttributes().put(attrName, attrValue);
          }
        }

        result.add(e);
      }

      return result;
    } catch (Exception ex) {
      throw new WebApplicationException(
          Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build());
    }
  }

  @Override
  public void updateCacheManager(@Context UriInfo info,
                                 CacheManagerEntity resource) {
    throw new WebApplicationException(
        Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Not yet implemented").build());
  }

  private List<ObjectName> findCacheManagerObjectNames(MBeanServer platformMBeanServer,
                                                       String[] cacheManagerNamesArg)
      throws MalformedObjectNameException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException,
      MBeanException {
    Map<String, ObjectName> allCacheManagerObjectNamesByCacheManagerName = CacheManagerJmxClient
        .getAllCacheManagerObjectNamesByCacheManagerName(platformMBeanServer);

    List<ObjectName> cacheManagerObjectNames = new ArrayList<ObjectName>();
    if (cacheManagerNamesArg == null) {
      for (ObjectName cacheManagerObjectName : allCacheManagerObjectNamesByCacheManagerName.values()) {
        cacheManagerObjectNames.add(cacheManagerObjectName);
      }
    } else {
      for (String name : cacheManagerNamesArg) {
        ObjectName objectName = allCacheManagerObjectNamesByCacheManagerName.get(name);
        cacheManagerObjectNames.add(objectName);
      }
    }
    return cacheManagerObjectNames;
  }

}
