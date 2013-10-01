package net.sf.ehcache.management.service;

import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;

import org.terracotta.management.ServiceExecutionException;

import java.util.Collection;
import java.util.Set;

/**
 * A factory interface for resources related to Ehcache.
 *
 * @author brandony
 */
public interface EntityResourceFactory {

  /**
   * A factory method for {@link CacheManagerEntity} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager} objects to be represented by the
   *                          returned resources
   * @param attributes        a {@code Set} of specific cache manager attributes to include in the returned representations;
   *                          if null, all attributes will be included
   * @return a {@code Collection} of {@code CacheManagerEntity} objects
   */
  Collection<CacheManagerEntity> createCacheManagerEntities(Set<String> cacheManagerNames,
                                                            Set<String> attributes) throws ServiceExecutionException;

  /**
   * A factory method for {@link CacheManagerConfigEntity} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager} configurations to be represented by the
   *                          returned resources
   * @return a {@code Collection} of {@code CacheManagerConfigEntity} objects
   */
  Collection<CacheManagerConfigEntity> createCacheManagerConfigEntities(Set<String> cacheManagerNames) throws ServiceExecutionException;

  /**
   * A factory method for {@link CacheEntity} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager}s that manage the {@link Cache}
   *                          objects to be represented by the returned resources
   * @param cacheNames        a {@code Set} of names for the {@link Cache} objects to be represented by the
   *                          returned resources
   * @param attributes        a {@code Set} of specific cache manager attributes to include in the returned representations;
   *                          if null, all attributes will be included
   * @return a {@code Collection} of {@code CacheEntity} objects
   */
  Collection<CacheEntity> createCacheEntities(Set<String> cacheManagerNames,
                                              Set<String> cacheNames,
                                              Set<String> attributes) throws ServiceExecutionException;

  /**
   * A factory method for {@link CacheConfigEntity} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager}s that manage the {@link Cache}
   *                          objects to be represented by the returned resources
   * @param cacheNames        a {@code Set} of names for the {@link Cache} objects to be represented by the
   *                          returned resources
   * @return a {@code Collection} of {@code CacheConfigEntity} objects
   */
  Collection<CacheConfigEntity> createCacheConfigEntities(Set<String> cacheManagerNames,
                                                          Set<String> cacheNames) throws ServiceExecutionException;

  /**
   * A factory method for {@link CacheStatisticSampleEntity} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager}s that manage the {@link Cache}s whose
   *                          sampled statistics are to be represented by the returned resources
   * @param cacheNames        a {@code Set} of names for the {@link Cache}s whose sampled statistics are to be represented
   *                          by the returned resources
   * @param statNames         a {@code Set} of names for the sampled statistics to be represented by the returned resources
   * @return a {@code Collection} of {@code CacheStatisticSampleEntity} objects
   */
  Collection<CacheStatisticSampleEntity> createCacheStatisticSampleEntity(Set<String> cacheManagerNames,
                                                                          Set<String> cacheNames,
                                                                          Set<String> statNames) throws ServiceExecutionException;
}
