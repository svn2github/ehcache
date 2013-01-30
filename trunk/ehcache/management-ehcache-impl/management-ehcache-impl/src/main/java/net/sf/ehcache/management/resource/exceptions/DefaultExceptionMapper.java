package net.sf.ehcache.management.resource.exceptions;

import org.terracotta.management.resource.exceptions.ExceptionUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Ludovic Orban
 */
@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {

  @Override
  public Response toResponse(Exception exception) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(ExceptionUtils.toJsonError(exception)).build();
  }

}
