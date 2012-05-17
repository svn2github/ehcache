package net.sf.ehcache.management.service;

import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;

import java.util.Collection;
import java.util.Set;

/**
 * and retrieving the associated {@code CacheManagerSampler} objects
 *
 * @author brandony
 */
public interface EntityResourceFactory {
  interface Locator {
    EntityResourceFactory locateEntityResourceFactory();
  }

  Collection<CacheManagerEntity> createCacheManagerEntities(Set<String> cacheManagerNames,
                                                            Set<String> attributes);

  Collection<CacheManagerConfigEntity> createCacheManagerConfigEntities(Set<String> cacheManagerNames);

  Collection<CacheEntity> createCacheEntities(Set<String> cacheManagerNames,
                                              Set<String> cacheNames,
                                              Set<String> attributes);

  Collection<CacheConfigEntity> createCacheConfigEntities(Set<String> cacheManagerNames,
                                                          Set<String> cacheNames);

  Collection<CacheStatisticSampleEntity> createCacheStatisticSampleEntity(Set<String> cacheManagerNames,
                                                                          Set<String> cacheNames,
                                                                          Set<String> statName);
}
