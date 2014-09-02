/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheConfigEntityV2;
import net.sf.ehcache.management.sampled.CacheManagerSampler;

import org.terracotta.management.resource.AgentEntityV2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author brandony
 */
final class CacheConfigurationEntityBuilderV2 {

  private final Map<String, CacheManagerSampler> samplersByCName = new HashMap<String, CacheManagerSampler>();

  static CacheConfigurationEntityBuilderV2 createWith(CacheManagerSampler sampler,
      String cacheName) {
    return new CacheConfigurationEntityBuilderV2(sampler, cacheName);
  }

  private CacheConfigurationEntityBuilderV2(CacheManagerSampler sampler,
      String cacheName) {
    addSampler(sampler, cacheName);
  }

  CacheConfigurationEntityBuilderV2 add(CacheManagerSampler sampler,
      String cacheName) {
    addSampler(sampler, cacheName);
    return this;
  }

  Collection<CacheConfigEntityV2> build() {
    Collection<CacheConfigEntityV2> cces = new ArrayList<CacheConfigEntityV2>();

    for (Map.Entry<String, CacheManagerSampler> entry : samplersByCName.entrySet()) {
      CacheConfigEntityV2 cce = new CacheConfigEntityV2();

      String cacheName = entry.getKey();
      cce.setCacheName(cacheName);
      cce.setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
      // cce.setVersion(this.getClass().getPackage().getImplementationVersion());

      CacheManagerSampler sampler = entry.getValue();
      cce.setCacheManagerName(sampler.getName());
      cce.setXml(sampler.generateActiveConfigDeclaration(cacheName));

      cces.add(cce);
    }

    return cces;
  }

  private void addSampler(CacheManagerSampler sampler,
      String cacheName) {
    if (sampler == null) {
      throw new IllegalArgumentException("sampler == null");
    }

    if (cacheName == null) {
      throw new IllegalArgumentException("cacheName == null");
    }

    if(!Arrays.asList(sampler.getCacheNames()).contains(cacheName)) {
      throw new IllegalArgumentException(String.format("Invalid cache name: %s.", cacheName));
    }

    samplersByCName.put(cacheName, sampler);
  }
}
