/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public interface AgentService {

  /**
   * A locator interface for this service.
   */
  interface Locator {
    AgentService locateAgentService();
  }


  /**
   * Get a collection of agent entities known by this agent.
   * @param ids a set of IDs. If empty, this means all known agents.
   * @return
   * @throws ServiceExecutionException
   */
  Collection<AgentEntity> getAgents(Set<String> ids) throws ServiceExecutionException;


  Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException;
}
