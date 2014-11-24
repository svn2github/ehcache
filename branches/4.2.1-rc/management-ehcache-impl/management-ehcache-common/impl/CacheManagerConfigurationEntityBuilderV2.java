/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheManagerConfigEntityV2;
import net.sf.ehcache.management.sampled.CacheManagerSampler;

import org.terracotta.management.resource.AgentEntityV2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author brandony
 */
final class CacheManagerConfigurationEntityBuilderV2 {
  private final List<CacheManagerSampler> cmSamplers = new ArrayList<CacheManagerSampler>();

  static CacheManagerConfigurationEntityBuilderV2 createWith(CacheManagerSampler sampler) {
    return new CacheManagerConfigurationEntityBuilderV2(sampler);
  }

  private CacheManagerConfigurationEntityBuilderV2(CacheManagerSampler sampler) {
    addSampler(sampler);
  }

  CacheManagerConfigurationEntityBuilderV2 add(CacheManagerSampler sampler) {
    addSampler(sampler);
    return this;
  }

  Collection<CacheManagerConfigEntityV2> build() {
    Collection<CacheManagerConfigEntityV2> cmces = new ArrayList<CacheManagerConfigEntityV2>(cmSamplers.size());

    for (CacheManagerSampler sampler : cmSamplers) {
      CacheManagerConfigEntityV2 cmce = new CacheManagerConfigEntityV2();
      cmce.setCacheManagerName(sampler.getName());
      cmce.setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
      // cmce.setVersion(this.getClass().getPackage().getImplementationVersion());
      cmce.setXml(sampler.generateActiveConfigDeclaration());

      cmces.add(cmce);
    }

    return cmces;
  }

  private void addSampler(CacheManagerSampler sampler) {
    if (sampler == null) {
      throw new IllegalArgumentException("sampler == null");
    }
    cmSamplers.add(sampler);
  }
}
