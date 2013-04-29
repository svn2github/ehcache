/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.sampled.CacheSampler;
import net.sf.ehcache.management.service.AccessorPrefix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.AgentEntity;

/**
 * @author brandony
 */
final class CacheEntityBuilder extends ConstrainableEntityBuilderSupport<CacheSampler> {
  private static final Logger LOG = LoggerFactory.getLogger(CacheEntityBuilder.class);

  private static final String C_NAME_ACCESSOR = AccessorPrefix.get + "CacheName";

  private final Map<String, Set<CacheSampler>> samplersByCMName = new HashMap<String, Set<CacheSampler>>();

  static CacheEntityBuilder createWith(CacheSampler sampler,
                                       String cacheManagerName) {
    return new CacheEntityBuilder(sampler, cacheManagerName);
  }

  private CacheEntityBuilder(CacheSampler sampler,
                             String cacheManagerName) {
    addSampler(sampler, cacheManagerName);
  }

  CacheEntityBuilder add(CacheSampler sampler,
                         String cacheManagerName) {
    addSampler(sampler, cacheManagerName);
    return this;
  }

  CacheEntityBuilder add(Set<String> constraintAttributes) {
    addConstraints(constraintAttributes);
    return this;
  }

  Collection<CacheEntity> build() {
    Collection<CacheEntity> ces = new ArrayList<CacheEntity>(samplersByCMName.values().size());

    for (Map.Entry<String, Set<CacheSampler>> entry : samplersByCMName.entrySet()) {
      for (CacheSampler sampler : entry.getValue()) {
        CacheEntity ce = new CacheEntity();
        ce.setCacheManagerName(entry.getKey());
        ce.setName(sampler.getCacheName());
        ce.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
        ce.setVersion(this.getClass().getPackage().getImplementationVersion());

        if (getAttributeConstraints() != null && !getAttributeConstraints().isEmpty() && getAttributeConstraints()
            .size() < CacheSampler.class.getMethods().length) {
          buildAttributeMapByAttribute(CacheSampler.class, sampler, ce.getAttributes(), getAttributeConstraints(),
              C_NAME_ACCESSOR);
        } else {
          buildAttributeMapByApi(CacheSampler.class, sampler, ce.getAttributes(), getAttributeConstraints(),
              C_NAME_ACCESSOR);
        }

        ces.add(ce);
      }
    }

    return ces;
  }

  Logger getLog() {
    return LOG;
  }

  @Override
  protected Set<String> getExcludedAttributeNames(CacheSampler sampler) {
    if (sampler.isLocalHeapCountBased()) {
      Set<String> excludedNames = new HashSet<String>();
      excludedNames.add("LocalHeapSizeInBytes");
      excludedNames.add("LocalHeapSizeInBytesSample");
      return excludedNames;
    }
    return Collections.emptySet();
  }

  private void addSampler(CacheSampler sampler,
                          String cacheManagerName) {
    if (sampler == null) throw new IllegalArgumentException("sampler == null");

    if (cacheManagerName == null) throw new IllegalArgumentException("cacheManagerName == null");

    Set<CacheSampler> samplers = samplersByCMName.get(cacheManagerName);

    if (samplers == null) {
      samplers = new HashSet<CacheSampler>();
      samplersByCMName.put(cacheManagerName, samplers);
    }

    samplers.add(sampler);
  }
}
