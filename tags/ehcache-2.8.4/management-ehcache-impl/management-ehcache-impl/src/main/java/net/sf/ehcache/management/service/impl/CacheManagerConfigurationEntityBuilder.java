/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import org.terracotta.management.resource.AgentEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author brandony
 */
final class CacheManagerConfigurationEntityBuilder {
  private final List<CacheManagerSampler> cmSamplers = new ArrayList<CacheManagerSampler>();

  static CacheManagerConfigurationEntityBuilder createWith(CacheManagerSampler sampler) {
    return new CacheManagerConfigurationEntityBuilder(sampler);
  }

  private CacheManagerConfigurationEntityBuilder(CacheManagerSampler sampler) {
    addSampler(sampler);
  }

  CacheManagerConfigurationEntityBuilder add(CacheManagerSampler sampler) {
    addSampler(sampler);
    return this;
  }

  Collection<CacheManagerConfigEntity> build() {
    Collection<CacheManagerConfigEntity> cmces = new ArrayList<CacheManagerConfigEntity>(cmSamplers.size());

    for (CacheManagerSampler sampler : cmSamplers) {
      CacheManagerConfigEntity cmce = new CacheManagerConfigEntity();
      cmce.setCacheManagerName(sampler.getName());
      cmce.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      cmce.setVersion(this.getClass().getPackage().getImplementationVersion());
      cmce.setXml(sampler.generateActiveConfigDeclaration());

      cmces.add(cmce);
    }

    return cmces;
  }

  private void addSampler(CacheManagerSampler sampler) {
    if (sampler == null) throw new IllegalArgumentException("sampler == null");
    cmSamplers.add(sampler);
  }
}
