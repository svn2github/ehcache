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

import net.sf.ehcache.management.resource.CacheEntityV2;
import net.sf.ehcache.management.sampled.CacheSampler;
import net.sf.ehcache.management.service.AccessorPrefix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.AgentEntityV2;

/**
 * @author brandony
 */
final class CacheEntityBuilderV2 extends ConstrainableEntityBuilderSupportV2<CacheSampler> {
  private static final Logger LOG = LoggerFactory.getLogger(CacheEntityBuilderV2.class);

  private static final String C_NAME_ACCESSOR = AccessorPrefix.get + "CacheName";

  private final Map<String, Set<CacheSampler>> samplersByCMName = new HashMap<String, Set<CacheSampler>>();

  static CacheEntityBuilderV2 createWith(CacheSampler sampler,
      String cacheManagerName) {
    return new CacheEntityBuilderV2(sampler, cacheManagerName);
  }

  private CacheEntityBuilderV2(CacheSampler sampler,
      String cacheManagerName) {
    addSampler(sampler, cacheManagerName);
  }

  CacheEntityBuilderV2 add(CacheSampler sampler,
      String cacheManagerName) {
    addSampler(sampler, cacheManagerName);
    return this;
  }

  CacheEntityBuilderV2 add(Set<String> constraintAttributes) {
    addConstraints(constraintAttributes);
    return this;
  }

  Collection<CacheEntityV2> build() {
    Collection<CacheEntityV2> ces = new ArrayList<CacheEntityV2>(samplersByCMName.values().size());

    for (Map.Entry<String, Set<CacheSampler>> entry : samplersByCMName.entrySet()) {
      for (CacheSampler sampler : entry.getValue()) {
        CacheEntityV2 ce = new CacheEntityV2();
        ce.setCacheManagerName(entry.getKey());
        ce.setName(sampler.getCacheName());
        ce.setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
        // ce.setVersion(this.getClass().getPackage().getImplementationVersion());

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

  @Override
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
    if (sampler == null) {
      throw new IllegalArgumentException("sampler == null");
    }

    if (cacheManagerName == null) {
      throw new IllegalArgumentException("cacheManagerName == null");
    }

    Set<CacheSampler> samplers = samplersByCMName.get(cacheManagerName);

    if (samplers == null) {
      samplers = new HashSet<CacheSampler>();
      samplersByCMName.put(cacheManagerName, samplers);
    }

    samplers.add(sampler);
  }
}
