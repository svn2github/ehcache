/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;
import net.sf.ehcache.management.sampled.CacheManagerSampler;
import net.sf.ehcache.management.sampled.CacheManagerSamplerImpl;
import net.sf.ehcache.management.sampled.ComprehensiveCacheSampler;
import net.sf.ehcache.management.sampled.ComprehensiveCacheSamplerImpl;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import net.sf.ehcache.management.service.SamplerRepositoryService;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A controller class registering new {@link CacheManager}.
 * </p>
 * An {@link EntityResourceFactory} implementation that interacts with the native Ehcache API.
 * </p>
 * A {@link CacheService} implementation that interacts with the native Ehcache API to manipulate {@link Cache}
 * objects.
 *
 * @author brandony
 */
public final class DfltSamplerRepositoryService
    implements SamplerRepositoryService, EntityResourceFactory, CacheService {
  /**
   * Guarded By cacheManagerSamplerRepoLock
   */
  private final Map<String, SamplerRepoEntry> cacheManagerSamplerRepo = new HashMap<String, SamplerRepoEntry>();

  private final ReadWriteLock cacheManagerSamplerRepoLock = new ReentrantReadWriteLock();

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(CacheManager cacheManager) {
    String cmName = cacheManager.getName();
    cacheManagerSamplerRepoLock.writeLock().lock();

    try {
      if (!cacheManagerSamplerRepo.containsKey(cmName)) {
        SamplerRepoEntry entry = new SamplerRepoEntry(cacheManager);
        cacheManager.setCacheManagerEventListener(entry);
        cacheManagerSamplerRepo.put(cmName, entry);
      }
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void unregister(CacheManager cacheManager) {
    cacheManagerSamplerRepoLock.writeLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.remove(cacheManager.getName());
      entry.destroy();
    } finally {
      cacheManagerSamplerRepoLock.writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasRegistered() {
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      return !cacheManagerSamplerRepo.isEmpty();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<CacheManagerEntity> createCacheManagerEntities(Set<String> cacheManagerNames,
                                                                   Set<String> attributes) {
    CacheManagerEntityBuilder builder = null;
    Collection<CacheManagerEntity> entities;
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (SamplerRepoEntry entry : cacheManagerSamplerRepo.values()) {
          builder = builder == null ? CacheManagerEntityBuilder.createWith(entry.getCacheManagerSampler()) : builder
              .add(entry.getCacheManagerSampler());
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null) {
            builder = builder == null ? CacheManagerEntityBuilder.createWith(entry.getCacheManagerSampler()) : builder
                .add(entry.getCacheManagerSampler());
          }
        }
      }
      if (builder == null) entities = new HashSet<CacheManagerEntity>(0);
      else entities = attributes == null ? builder.build() : builder.add(attributes).build();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    return entities;
  }

  @Override
  public Collection<CacheManagerConfigEntity> createCacheManagerConfigEntities(Set<String> cacheManagerNames) {
    CacheManagerConfigurationEntityBuilder builder = null;
    Collection<CacheManagerConfigEntity> entities;

    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (SamplerRepoEntry entry : cacheManagerSamplerRepo.values()) {
          builder = builder == null ? CacheManagerConfigurationEntityBuilder
              .createWith(entry.getCacheManagerSampler()) : builder.add(entry.getCacheManagerSampler());
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);

          if (entry != null) {
            builder = builder == null ? CacheManagerConfigurationEntityBuilder
                .createWith(entry.getCacheManagerSampler()) : builder.add(entry.getCacheManagerSampler());
          }
        }
      }
      if (builder == null) entities = new HashSet<CacheManagerConfigEntity>(0);
      else entities = builder.build();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    return entities;
  }

  @Override
  public Collection<CacheEntity> createCacheEntities(Set<String> cacheManagerNames,
                                                     Set<String> cacheNames,
                                                     Set<String> attributes) {
    CacheEntityBuilder builder = null;
    Collection<CacheEntity> entities;

    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          for (ComprehensiveCacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            builder = builder == null ? CacheEntityBuilder.createWith(sampler, entry.getKey()) : builder
                .add(sampler, entry.getKey());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null) {
            for (ComprehensiveCacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder = builder == null ? CacheEntityBuilder.createWith(sampler, cmName) : builder.add(sampler, cmName);
            }
          }
        }
      }
      if (builder == null) entities = new HashSet<CacheEntity>(0);
      else entities = attributes == null ? builder.build() : builder.add(attributes).build();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    return entities;
  }

  @Override
  public Collection<CacheConfigEntity> createCacheConfigEntities(Set<String> cacheManagerNames,
                                                                 Set<String> cacheNames) {
    CacheConfigurationEntityBuilder builder = null;
    Collection<CacheConfigEntity> entities;

    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          for (ComprehensiveCacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            builder = builder == null ? CacheConfigurationEntityBuilder
                .createWith(entry.getValue().getCacheManagerSampler(), sampler.getCacheName()) : builder
                .add(entry.getValue().getCacheManagerSampler(), sampler.getCacheName());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null) {
            for (ComprehensiveCacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder = builder == null ? CacheConfigurationEntityBuilder
                  .createWith(entry.getCacheManagerSampler(), sampler.getCacheName()) : builder
                  .add(entry.getCacheManagerSampler(), sampler.getCacheName());
            }
          }
        }
      }
      if (builder == null) entities = new HashSet<CacheConfigEntity>(0);
      else entities = builder.build();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

    return entities;
  }

  @Override
  public Collection<CacheStatisticSampleEntity> createCacheStatisticSampleEntity(Set<String> cacheManagerNames,
                                                                                 Set<String> cacheNames,
                                                                                 Set<String> sampleNames) {
    CacheStatisticSampleEntityBuilder builder = CacheStatisticSampleEntityBuilder.createWith(sampleNames);

    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      if (cacheManagerNames == null) {
        for (Map.Entry<String, SamplerRepoEntry> entry : cacheManagerSamplerRepo.entrySet()) {
          for (ComprehensiveCacheSampler sampler : entry.getValue().getComprehensiveCacheSamplers(cacheNames)) {
            builder.add(sampler, entry.getKey());
          }
        }
      } else {
        for (String cmName : cacheManagerNames) {
          SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cmName);
          if (entry != null) {
            for (ComprehensiveCacheSampler sampler : entry.getComprehensiveCacheSamplers(cacheNames)) {
              builder.add(sampler, cmName);
            }
          }
        }
      }

      return builder.build();
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearCacheStats(String cacheManagerName,
                              String cacheName) {
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
      if (entry != null) entry.clearStats(cacheName);
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }

  @Override
  public void createOrUpdateCache(String cacheManagerName,
                                  String cacheName,
                                  CacheEntity resource) {
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
      if (entry != null) entry.updateCache(cacheName, resource);
      else throw new UnsupportedOperationException("Create cache not implemented!");
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }

  }

  @Override
  public void clearCache(String cacheManagerName,
                         String cacheName) {
    cacheManagerSamplerRepoLock.readLock().lock();

    try {
      SamplerRepoEntry entry = cacheManagerSamplerRepo.get(cacheManagerName);
      if (entry != null) entry.clearCache(cacheName);
    } finally {
      cacheManagerSamplerRepoLock.readLock().unlock();
    }
  }

  /**
   * The repository entry class that is also a {@link CacheManagerEventListener}.
   */
  private static final class SamplerRepoEntry implements CacheManagerEventListener {
    private final static String ENABLED_ATTR = "Enabled";

    private final static String STATS_ENABLED_ATTR = "StatisticsEnabled";

    private final static String SAMPLED_STATS_ENABLED_ATTR = "SampledStatisticsEnabled";

    private final static String BULK_LOAD_ENABLED = "ClusterBulkLoadEnabled";

    private CacheManager cacheManager;

    private CacheManagerSampler cacheManagerSampler;

    /**
     * Guarded by cacheSamplerMapLock
     */
    private Map<String, ComprehensiveCacheSampler> cacheSamplersByName;

    private volatile Status status = Status.STATUS_UNINITIALISED;

    private final ReadWriteLock cacheSamplerMapLock = new ReentrantReadWriteLock();

    public SamplerRepoEntry(CacheManager cacheManager) {
      if (cacheManager == null) throw new IllegalArgumentException("cacheManager == null");

      this.cacheManagerSampler = new CacheManagerSamplerImpl(cacheManager);
      this.cacheManager = cacheManager;

      String[] cNames = cacheManager.getCacheNames();
      this.cacheSamplersByName = new HashMap<String, ComprehensiveCacheSampler>(cNames.length);

      for (String cName : cNames)
        cacheSamplersByName.put(cName, new ComprehensiveCacheSamplerImpl(cacheManager.getCache(cName)));
    }

    public CacheManagerSampler getCacheManagerSampler() {
      return cacheManagerSampler;
    }

    public Collection<ComprehensiveCacheSampler> getComprehensiveCacheSamplers(Set<String> cacheSamplerNames) {
      Collection<ComprehensiveCacheSampler> samplers = new HashSet<ComprehensiveCacheSampler>();

      cacheSamplerMapLock.readLock().lock();
      try {
        if (cacheSamplerNames == null) {
          for (ComprehensiveCacheSampler cs : cacheSamplersByName.values()) {
            samplers.add(cs);
          }
        } else {
          for (String cName : cacheSamplerNames) {
            ComprehensiveCacheSampler cs = cacheSamplersByName.get(cName);
            if (cs != null) samplers.add(cs);
          }
        }
      } finally {
        cacheSamplerMapLock.readLock().unlock();
      }

      return samplers;
    }

    public void clearStats(String cacheSamplerName) {
      cacheSamplerMapLock.writeLock().lock();

      ComprehensiveCacheSampler cs;
      try {
        cs = cacheSamplersByName.get(cacheSamplerName);
        if (cs != null) cs.clearStatistics();
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    public void clearCache(String cacheSamplerName) {
      cacheSamplerMapLock.writeLock().lock();

      ComprehensiveCacheSampler cs;
      try {
        cs = cacheSamplersByName.get(cacheSamplerName);
        if (cs != null) cs.removeAll();
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    public void updateCache(String cacheSamplerName,
                            CacheEntity cacheResource) {
      cacheSamplerMapLock.writeLock().lock();

      ComprehensiveCacheSampler cs;
      try {
        cs = cacheSamplersByName.get(cacheSamplerName);

        if (cs != null) {
          Object enabledAttr = cacheResource.getAttributes().get(ENABLED_ATTR);
          if (enabledAttr != null) cs.setEnabled(Boolean.parseBoolean(enabledAttr.toString()));

          Object enabledBlkLoad = cacheResource.getAttributes().get(BULK_LOAD_ENABLED);
          if (enabledBlkLoad != null) cs.setNodeBulkLoadEnabled(Boolean.parseBoolean(enabledBlkLoad.toString()));

          Object enabledStatsAttr = cacheResource.getAttributes().get(STATS_ENABLED_ATTR);
          if (enabledStatsAttr != null) cs.setStatisticsEnabled(Boolean.parseBoolean(enabledStatsAttr.toString()));

          Object enabledSampledStatsAttr = cacheResource.getAttributes().get(SAMPLED_STATS_ENABLED_ATTR);
          if (enabledSampledStatsAttr != null) {
            boolean enableSampledStats = Boolean.parseBoolean(enabledSampledStatsAttr.toString());
            if (enableSampledStats) cs.enableSampledStatistics();
            else cs.disableSampledStatistics();
          }
        }
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws CacheException {
      status = Status.STATUS_ALIVE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status getStatus() {
      return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() throws CacheException {
      cacheSamplerMapLock.writeLock().lock();

      try {
        cacheSamplersByName.clear();
        cacheSamplersByName = null;
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }

      status = Status.STATUS_SHUTDOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyCacheAdded(String cacheName) {
      cacheSamplerMapLock.writeLock().lock();
      try {
        Cache c = cacheManager.getCache(cacheName);

        if (c != null) {
          cacheSamplersByName.put(cacheName, new ComprehensiveCacheSamplerImpl(c));
        }
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyCacheRemoved(String cacheName) {
      cacheSamplerMapLock.writeLock().lock();

      try {
        cacheSamplersByName.remove(cacheName);
      } finally {
        cacheSamplerMapLock.writeLock().unlock();
      }
    }

    public void destroy() {
      cacheManagerSampler = null;
      cacheManager = null;
    }
  }

}
