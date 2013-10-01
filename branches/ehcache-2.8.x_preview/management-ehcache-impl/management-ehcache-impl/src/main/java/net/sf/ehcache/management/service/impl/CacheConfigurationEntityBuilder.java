/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import org.terracotta.management.resource.AgentEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author brandony
 */
final class CacheConfigurationEntityBuilder {

  private final Map<String, CacheManagerSampler> samplersByCName = new HashMap<String, CacheManagerSampler>();

  static CacheConfigurationEntityBuilder createWith(CacheManagerSampler sampler,
                                                           String cacheName) {
    return new CacheConfigurationEntityBuilder(sampler, cacheName);
  }

  private CacheConfigurationEntityBuilder(CacheManagerSampler sampler,
                                          String cacheName) {
    addSampler(sampler, cacheName);
  }

  CacheConfigurationEntityBuilder add(CacheManagerSampler sampler,
                                             String cacheName) {
    addSampler(sampler, cacheName);
    return this;
  }

  Collection<CacheConfigEntity> build() {
    Collection<CacheConfigEntity> cces = new ArrayList<CacheConfigEntity>();

    for (Map.Entry<String, CacheManagerSampler> entry : samplersByCName.entrySet()) {
      CacheConfigEntity cce = new CacheConfigEntity();

      String cacheName = entry.getKey();
      cce.setCacheName(cacheName);
      cce.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      cce.setVersion(this.getClass().getPackage().getImplementationVersion());

      CacheManagerSampler sampler = entry.getValue();
      cce.setCacheManagerName(sampler.getName());
      cce.setXml(sampler.generateActiveConfigDeclaration(cacheName));

      cces.add(cce);
    }

    return cces;
  }

  private void addSampler(CacheManagerSampler sampler,
                          String cacheName) {
    if (sampler == null) throw new IllegalArgumentException("sampler == null");

    if (cacheName == null) throw new IllegalArgumentException("cacheName == null");

    if(!Arrays.asList(sampler.getCacheNames()).contains(cacheName)) {
      throw new IllegalArgumentException(String.format("Invalid cache name: %s.", cacheName));
    }

    samplersByCName.put(cacheName, sampler);
  }
}
