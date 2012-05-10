package net.sf.ehcache.management.validators;

import org.terracotta.management.resource.services.Utils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author brandony
 */
public final class EhcacheValidationUtils {
  public static void validateUnsafeCacheManagerRequest(UriInfo info) {
    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = Utils.trimToNull(cacheManagerNames) == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));
    if (cmNames == null) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
          "No cache manager specified. Unsafe requests must specify a single cache manager name.").build());
    }

    if (cmNames.size() != 1) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
          "Multiple cache managers specified. Unsafe requests must specify a single cache manager name.").build());
    }
  }

  public static void validateUnsafeCacheRequest(UriInfo info) {
    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));
    if (cNames == null) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
          "No cache specified. Unsafe requests must specify a single cache manager name.").build());
    }

    if (cNames.size() != 1) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
          "Multiple caches specified. Unsafe requests must specify a single cache manager name.").build());
    }
  }
}
