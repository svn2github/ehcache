/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services.validator.impl;

import net.sf.ehcache.management.resource.services.validator.AbstractEhcacheRequestValidatorV2;

import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.Utils;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.List;

/**
 * A concrete implementation of ehcache resource service {@link net.sf.ehcache.management.resource.services.validator.AbstractEhcacheRequestValidator} for the embedded agent.
 * <p/>
 * {@inheritDoc}
 *
 * @author brandony
 */
public final class EmbeddedEhcacheRequestValidatorV2 extends AbstractEhcacheRequestValidatorV2 {

  /**
   * {@inheritDoc}
   */
  @Override
  public void validateSafe(UriInfo info) {
    validateAgentSegment(info.getPathSegments());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void validateAgentSegment(List<PathSegment> pathSegments) {
    String ids = pathSegments.get(0).getMatrixParameters().getFirst("ids");

    if (Utils.trimToNull(ids) != null && !AgentEntityV2.EMBEDDED_AGENT_ID.equals(ids)) {
      throw new ResourceRuntimeException(String.format("Agent ID must be '%s'.", AgentEntityV2.EMBEDDED_AGENT_ID),
          Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
