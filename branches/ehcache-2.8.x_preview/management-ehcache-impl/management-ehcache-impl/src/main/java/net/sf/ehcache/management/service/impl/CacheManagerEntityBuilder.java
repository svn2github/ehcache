/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import net.sf.ehcache.management.service.AccessorPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.AgentEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author brandony
 */
final class CacheManagerEntityBuilder extends ConstrainableEntityBuilderSupport<CacheManagerSampler> {
  private static final Logger LOG = LoggerFactory.getLogger(CacheManagerEntityBuilder.class);

  private static final String CM_NAME_ACCESSOR = AccessorPrefix.get + "Name";

  private final List<CacheManagerSampler> cmSamplers = new ArrayList<CacheManagerSampler>();

  static CacheManagerEntityBuilder createWith(CacheManagerSampler sampler) {
    return new CacheManagerEntityBuilder(sampler);
  }

  CacheManagerEntityBuilder(CacheManagerSampler sampler) {
    addSampler(sampler);
  }

  CacheManagerEntityBuilder add(CacheManagerSampler sampler) {
    addSampler(sampler);
    return this;
  }

  CacheManagerEntityBuilder add(Set<String> constraintAttributes) {
    addConstraints(constraintAttributes);
    return this;
  }

  Collection<CacheManagerEntity> build() {
    Collection<CacheManagerEntity> cmes = new ArrayList<CacheManagerEntity>(cmSamplers.size());

    for (CacheManagerSampler cms : cmSamplers) {
      CacheManagerEntity cme = new CacheManagerEntity();
      cme.setName(cms.getName());
      cme.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      cme.setVersion(this.getClass().getPackage().getImplementationVersion());

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

  Logger getLog() {
    return LOG;
  }

  @Override
  protected Set<String> getExcludedAttributeNames(CacheManagerSampler cacheManagerSampler) {
    return Collections.emptySet();
  }

  private void addSampler(CacheManagerSampler sampler) {
    if (sampler == null) throw new IllegalArgumentException("sampler == null");
    cmSamplers.add(sampler);
  }
}
