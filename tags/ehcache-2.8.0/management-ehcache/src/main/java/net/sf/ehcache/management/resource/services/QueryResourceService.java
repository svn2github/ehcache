package net.sf.ehcache.management.resource.services;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import net.sf.ehcache.management.resource.QueryResultsEntity;

public interface QueryResourceService {
  public final static String ATTR_QUERY_KEY = "text";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<QueryResultsEntity> executeQuery(@Context UriInfo info);
}
