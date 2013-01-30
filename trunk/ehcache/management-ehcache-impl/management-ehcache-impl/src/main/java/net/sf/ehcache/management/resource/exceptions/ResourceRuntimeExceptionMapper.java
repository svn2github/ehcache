package net.sf.ehcache.management.resource.exceptions;

import org.terracotta.management.resource.exceptions.ExceptionUtils;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Ludovic Orban
 */
@Provider
public class ResourceRuntimeExceptionMapper implements ExceptionMapper<ResourceRuntimeException> {

  @Override
  public Response toResponse(ResourceRuntimeException exception) {
    return Response.status(exception.getStatusCode())
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(ExceptionUtils.toJsonError(exception)).build();
  }

}
