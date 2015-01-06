/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheManagerEntityV2;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import net.sf.ehcache.management.service.AccessorPrefix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.AgentEntityV2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author brandony
 */
final class CacheManagerEntityBuilderV2 extends ConstrainableEntityBuilderSupportV2<CacheManagerSampler> {
  private static final Logger LOG = LoggerFactory.getLogger(CacheManagerEntityBuilderV2.class);

  private static final String CM_NAME_ACCESSOR = AccessorPrefix.get + "Name";

  private final List<CacheManagerSampler> cmSamplers = new ArrayList<CacheManagerSampler>();

  static CacheManagerEntityBuilderV2 createWith(CacheManagerSampler sampler) {
    return new CacheManagerEntityBuilderV2(sampler);
  }

  CacheManagerEntityBuilderV2(CacheManagerSampler sampler) {
    addSampler(sampler);
  }

  CacheManagerEntityBuilderV2 add(CacheManagerSampler sampler) {
    addSampler(sampler);
    return this;
  }

  CacheManagerEntityBuilderV2 add(Set<String> constraintAttributes) {
    addConstraints(constraintAttributes);
    return this;
  }

  Collection<CacheManagerEntityV2> build() {
    Collection<CacheManagerEntityV2> cmes = new ArrayList<CacheManagerEntityV2>(cmSamplers.size());

    for (CacheManagerSampler cms : cmSamplers) {
      CacheManagerEntityV2 cme = new CacheManagerEntityV2();
      cme.setName(cms.getName());
      cme.setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
      // cme.setVersion(this.getClass().getPackage().getImplementationVersion());

      if (getAttributeConstraints() != null && !getAttributeConstraints().isEmpty() && getAttributeConstraints()
          .size() < CacheManagerSampler.class.getMethods().length) {
        buildAttributeMapByAttribute(CacheManagerSampler.class, cms, cme.getAttributes(), getAttributeConstraints(),
            CM_NAME_ACCESSOR);
      } else {
        buildAttributeMapByApi(CacheManagerSampler.class, cms, cme.getAttributes(), getAttributeConstraints(),
            CM_NAME_ACCESSOR);
      }

      cmes.add(cme);
    }

    return cmes;
  }

  @Override
  Logger getLog() {
    return LOG;
  }

  @Override
  protected Set<String> getExcludedAttributeNames(CacheManagerSampler cacheManagerSampler) {
    return Collections.emptySet();
  }

  private void addSampler(CacheManagerSampler sampler) {
    if (sampler == null) {
      throw new IllegalArgumentException("sampler == null");
    }
    cmSamplers.add(sampler);
  }
}
