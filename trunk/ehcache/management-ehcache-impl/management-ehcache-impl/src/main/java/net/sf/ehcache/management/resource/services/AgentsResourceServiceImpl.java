/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.sf.ehcache.management.service.AgentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.AgentsResourceService;
import org.terracotta.management.resource.services.validator.RequestValidator;

/**
 * An embedded implementation of {@link AgentsResourceService}.
 * <p/>
 *
 * @author brandony
 */
@Path("/agents")
public final class AgentsResourceServiceImpl implements AgentsResourceService {
  private static final Logger LOG = LoggerFactory.getLogger(AgentsResourceServiceImpl.class);

  private final AgentService agentService;

  private final RequestValidator validator;

  public AgentsResourceServiceImpl() {
    this.agentService = ServiceLocator.locate(AgentService.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
  }

  /**
   * {@inheritDoc}
   */
  public Collection<AgentEntity> getAgents(UriInfo info) {
    LOG.debug(String.format("Invoking AgentsResourceServiceImpl.getAgents: %s", info.getRequestUri()));

    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");
    Set<String> idSet;
    if (ids == null) {
      idSet = Collections.emptySet();
    } else {
      idSet = new HashSet<String>(Arrays.asList(ids.split(",")));
    }

    try {
      return agentService.getAgents(idSet);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get agents", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(UriInfo info) {
    LOG.debug(String.format("Invoking AgentsResourceServiceImpl.getAgentsMetadata: %s", info.getRequestUri()));

    validator.validateSafe(info);
    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");
    Set<String> idSet;
    if (ids == null) {
      idSet = Collections.emptySet();
    } else {
      idSet = new HashSet<String>(Arrays.asList(ids.split(",")));
    }

    try {
      return agentService.getAgentsMetadata(idSet);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get agents metadata", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
