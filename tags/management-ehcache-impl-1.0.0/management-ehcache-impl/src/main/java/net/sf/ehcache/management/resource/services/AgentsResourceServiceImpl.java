/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.EmbeddedEhcacheServiceLocator;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.services.AgentsResourceService;
import org.terracotta.management.resource.services.Utils;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An embedded implementation of {@link AgentsResourceService}.
 * <p/>
 *
 * @author brandony
 */
@Path("/agents")
public final class AgentsResourceServiceImpl implements AgentsResourceService {
  private static final Logger LOG = LoggerFactory.getLogger(AgentsResourceServiceImpl.class);

  private final static Set<String> DFLT_ATTRS = new HashSet<String>(Arrays.asList(new String[]{"Name"}));

  private final EntityResourceFactory entityResourceFactory;

  private final RequestValidator validator;

  private final ManagementRESTServiceConfiguration mgmtRESTSvcConfig;

  public AgentsResourceServiceImpl() {
    EntityResourceFactory.Locator entityRsrcFactoryLocator = EmbeddedEhcacheServiceLocator.locator();
    this.entityResourceFactory = entityRsrcFactoryLocator.locateEntityResourceFactory();
    RequestValidator.Locator reqValidatorLocator = EmbeddedEhcacheServiceLocator.locator();
    this.validator = reqValidatorLocator.locateRequestValidator();
    this.mgmtRESTSvcConfig = EmbeddedEhcacheServiceLocator.locator().locateRESTConfiguration();
  }

  /**
   * {@inheritDoc}
   */
  public Collection<AgentEntity> getAgents(UriInfo info) {
    LOG.info(String.format("Invoking AgentsResourceServiceImpl.getAgents: %s", info.getRequestUri()));

    validator.validateSafe(info);

    return buildAgent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(UriInfo info) {
    LOG.info(String.format("Invoking AgentsResourceServiceImpl.getAgentsMetadata: %s", info.getRequestUri()));

    validator.validateSafe(info);

    AgentMetadataEntity ame = new AgentMetadataEntity();
    ame.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    ame.setAgencyOf("Ehcache");
    ame.setVersion(this.getClass().getPackage().getImplementationVersion());
    ame.setAvailable(true);
    ame.setSecured(Utils.trimToNull(mgmtRESTSvcConfig.getSecurityServiceLocation()) != null);
    ame.setLicensed(EmbeddedEhcacheServiceLocator.locator().isLicensedLocator());
    ame.setNeedClientAuth(mgmtRESTSvcConfig.isNeedClientAuth());
    ame.setSampleHistorySize(mgmtRESTSvcConfig.getSampleHistorySize());
    ame.setSampleIntervalSeconds(mgmtRESTSvcConfig.getSampleIntervalSeconds());
    return Collections.singleton(ame);
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
