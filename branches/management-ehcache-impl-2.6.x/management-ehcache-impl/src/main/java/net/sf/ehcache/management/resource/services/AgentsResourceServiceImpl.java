/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource.services;

import com.sun.jersey.api.core.InjectParam;
import net.sf.ehcache.management.services.EntityResourceFactory;
import net.sf.ehcache.management.validators.impl.AgentRequestValidator;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.services.AgentsResourceService;
import org.terracotta.management.resource.services.Utils;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * An embedded implementation of {@link AgentsResourceService}.
 * </p>
 *
 * @author brandony
 */
@Path("/agents")
public final class AgentsResourceServiceImpl implements AgentsResourceService {
  private final static Set<String> DFLT_ATTRS = new HashSet<String>(Arrays.asList(new String[]{"Name"}));
  private final EntityResourceFactory entityResourceFactory;

  private final AgentRequestValidator validator;

  public AgentsResourceServiceImpl(@InjectParam EntityResourceFactory entityResourceFactory,
                                   @InjectParam AgentRequestValidator validator) {
    this.entityResourceFactory = entityResourceFactory;
    this.validator = validator;
  }

  /**
   * {@inheritDoc}
   */
  public Response getAgents(UriInfo info) {
    validator.validateSafe(info);

    return Utils.buildNoCacheResponse(buildAgent());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Response getAgentsMetadata(UriInfo info) {
    validator.validateSafe(info);

    AgentMetadataEntity ame = new AgentMetadataEntity();
    ame.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    // TODO: I imagine there is some specific ehcache naming detail we can discover to build up a more descriptive name.
    // If this ends up being a static value then make it a static member.
    ame.setAgencyOf("Ehcache");
    // Set the version from this package
    ame.setVersion(this.getClass().getPackage().getImplementationVersion());
    ame.setAvailable(true);
    return Utils.buildNoCacheResponse(Collections.singleton(ame));
  }

  private Collection<AgentEntity> buildAgent() {
    AgentEntity e = new AgentEntity();
    e.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);

    Collection<Representable> reps = new HashSet<Representable>();
    reps.addAll(entityResourceFactory.createCacheManagerEntities(null, DFLT_ATTRS));
    e.setRootRepresentables(reps);

    return Collections.singleton(e);
  }
}
