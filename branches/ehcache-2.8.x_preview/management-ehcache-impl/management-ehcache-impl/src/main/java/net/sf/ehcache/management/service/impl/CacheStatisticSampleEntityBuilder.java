/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;
import net.sf.ehcache.management.sampled.CacheSampler;
import net.sf.ehcache.management.service.AccessorPrefix;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.TimeStampedCounterValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.AgentEntity;

/**
 * A builder for {@link CacheStatisticSampleEntity} resource objects.
 *
 * @author brandony
 */
final class CacheStatisticSampleEntityBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(CacheStatisticSampleEntityBuilder.class);

  private static final String SAMPLE_SUFFIX = "Sample";

  private final Set<String> sampleNames;

  private final Map<String, Set<CacheSampler>> samplersByCMName = new HashMap<String, Set<CacheSampler>>();

  static CacheStatisticSampleEntityBuilder createWith(Set<String> statisticSampleName) {
    return new CacheStatisticSampleEntityBuilder(statisticSampleName);
  }

  private CacheStatisticSampleEntityBuilder(Set<String> sampleNames) {
    this.sampleNames = sampleNames;
  }

  CacheStatisticSampleEntityBuilder add(CacheSampler sampler,
                                        String cacheManagerName) {
    addSampler(sampler, cacheManagerName);
    return this;
  }

  Collection<CacheStatisticSampleEntity> build() {
    Collection<CacheStatisticSampleEntity> csses = new ArrayList<CacheStatisticSampleEntity>();

    for (Map.Entry<String, Set<CacheSampler>> entry : samplersByCMName.entrySet()) {
      for (CacheSampler sampler : entry.getValue()) {
        if (sampleNames == null) {
          for (Method m : CacheSampler.class.getMethods()) {
            if (AccessorPrefix.isAccessor(m.getName()) && SampledCounter.class.isAssignableFrom(m.getReturnType())) {
              CacheStatisticSampleEntity csse = makeStatResource(m, sampler, entry.getKey());
              if (csse != null) csses.add(csse);
            }
          }
        } else {
          for (String sampleName : sampleNames) {
            Method sampleMethod;
            try {
              sampleMethod = CacheSampler.class.getMethod(AccessorPrefix.get + sampleName + SAMPLE_SUFFIX);
            } catch (NoSuchMethodException e) {
              LOG.warn("A statistic sample with the name '{}' does not exist.", sampleName);
              continue;
            }

            if (SampledCounter.class.isAssignableFrom(sampleMethod.getReturnType())) {
              CacheStatisticSampleEntity csse = makeStatResource(sampleMethod, sampler, entry.getKey());
              if (csse != null) csses.add(csse);
            }
          }
        }
      }
    }

    return csses;
  }

  private CacheStatisticSampleEntity makeStatResource(Method sampleMethod,
                                                      CacheSampler sampler,
                                                      String cmName) {
    SampledCounter sCntr;
    try {
      sCntr = SampledCounter.class.cast(sampleMethod.invoke(sampler));
    } catch (IllegalAccessException e) {
      LOG.warn("Failed to invoke method '{}' while constructing entity due to access restriction.",
          sampleMethod.getName());
      sCntr = null;
    } catch (InvocationTargetException e) {
      LOG.warn(String.format("Failed to invoke method %s while constructing entity.", sampleMethod.getName()), e);
      sCntr = null;
    }

    if (sCntr != null) {
      CacheStatisticSampleEntity csse = new CacheStatisticSampleEntity();
      csse.setCacheManagerName(cmName);
      csse.setName(sampler.getCacheName());
      csse.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      csse.setVersion(this.getClass().getPackage().getImplementationVersion());
      csse.setStatName(AccessorPrefix.trimPrefix(sampleMethod.getName()).replace(SAMPLE_SUFFIX, ""));

      TimeStampedCounterValue[] tscvs = sCntr.getAllSampleValues();
      Map<Long, Long> statValueByTime = new TreeMap<Long, Long>();
      csse.setStatValueByTimeMillis(statValueByTime);

      for (TimeStampedCounterValue tscv : tscvs) {
        statValueByTime.put(tscv.getTimestamp(), tscv.getCounterValue());
      }
      return csse;
    }

    return null;
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
