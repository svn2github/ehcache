package net.sf.ehcache.management.resource.services;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.sf.ehcache.management.service.CacheManagerServiceV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ExceptionUtils;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

/**
 * <p>An implementation of {@link QueryResourceService}.</p>
 *
 * @author gkeim
 */
@Path("/v2/agents/cacheManagers/query")
public final class QueryResourceServiceImplV2 {
  public final static String ATTR_QUERY_KEY = "text";

  private static final Logger LOG = LoggerFactory.getLogger(QueryResourceServiceImplV2.class);

  private final RequestValidator validator;

  private final CacheManagerServiceV2 cacheMgrSvc;

  public QueryResourceServiceImplV2() {
    this.validator = ServiceLocator.locate(RequestValidator.class);
    this.cacheMgrSvc = ServiceLocator.locate(CacheManagerServiceV2.class);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2 executeQuery(@Context UriInfo info) {
    LOG.debug(String.format("Invoking executeQuery: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String cacheManagerName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    List<String> querys = qParams.get(ATTR_QUERY_KEY);
    String queryString = querys.size() > 0 ? querys.get(0) : null;

    try {
      return cacheMgrSvc.executeQuery(cacheManagerName, queryString);
    } catch (ServiceExecutionException e) {
      Throwable t = ExceptionUtils.getRootCause(e);
      throw new ResourceRuntimeException("Failed to execute query", t, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
