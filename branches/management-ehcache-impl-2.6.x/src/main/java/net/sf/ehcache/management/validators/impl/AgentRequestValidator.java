package net.sf.ehcache.management.validators.impl;

import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.services.Utils;
import org.terracotta.management.validators.RequestValidator;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Named("embeddedAgentReqValidator")
public class AgentRequestValidator implements RequestValidator {

  @Override
  public void validateSafe(UriInfo info) {
    doValidation(info);
  }

  @Override
  public void validate(UriInfo info) {
    doValidation(info);
  }

  private void doValidation(UriInfo info) {
    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");

    if (Utils.trimToNull(ids) != null && !AgentEntity.EMBEDDED_AGENT_ID.equals(ids)) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(String.format("Agent ID must be '%s'.", AgentEntity.EMBEDDED_AGENT_ID)).build());
    }
  }
}
