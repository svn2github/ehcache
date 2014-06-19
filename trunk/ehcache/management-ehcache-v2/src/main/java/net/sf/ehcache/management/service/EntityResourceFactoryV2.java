package net.sf.ehcache.management.service;

import net.sf.ehcache.management.resource.CacheConfigEntityV2;
import net.sf.ehcache.management.resource.CacheEntityV2;
import net.sf.ehcache.management.resource.CacheManagerConfigEntityV2;
import net.sf.ehcache.management.resource.CacheManagerEntityV2;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntityV2;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import java.util.Collection;
import java.util.Set;

/**
 * A factory interface for resources related to Ehcache.
 *
 * @author brandony
 */
public interface EntityResourceFactoryV2 {

  /**
   * A factory method for {@link CacheManagerEntityV2} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager} objects to be represented by the
   *                          returned resources
   * @param attributes        a {@code Set} of specific cache manager attributes to include in the returned representations;
   *                          if null, all attributes will be included
   * @return a {@code ResponseEntityV2} containing {@code CacheManagerEntity} objects
   */
  ResponseEntityV2 createCacheManagerEntities(Set<String> cacheManagerNames,
                                                            Set<String> attributes) throws ServiceExecutionException;

  /**
   * A factory method for {@link CacheManagerConfigEntityV2} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager} configurations to be represented by the
   *                          returned resources
   * @return a {@code ResponseEntityV2} containing {@code CacheManagerConfigEntity} objects
   */
  ResponseEntityV2 createCacheManagerConfigEntities(Set<String> cacheManagerNames) throws ServiceExecutionException;

  /**
   * A factory method for {@link CacheEntityV2} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager}s that manage the {@link Cache}
   *                          objects to be represented by the returned resources
   * @param cacheNames        a {@code Set} of names for the {@link Cache} objects to be represented by the
   *                          returned resources
   * @param attributes        a {@code Set} of specific cache manager attributes to include in the returned representations;
   *                          if null, all attributes will be included
   * @return a {@code ResponseEntityV2} containing {@code CacheEntity} objects
   */
  ResponseEntityV2 createCacheEntities(Set<String> cacheManagerNames,
                                              Set<String> cacheNames,
                                              Set<String> attributes) throws ServiceExecutionException;

  /**
   * A factory method for {@link CacheConfigEntityV2} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager}s that manage the {@link Cache}
   *                          objects to be represented by the returned resources
   * @param cacheNames        a {@code Set} of names for the {@link Cache} objects to be represented by the
   *                          returned resources
   * @return a {@code ResponseEntityV2} containing {@code CacheConfigEntity} objects
   */
  ResponseEntityV2 createCacheConfigEntities(Set<String> cacheManagerNames,
                                                          Set<String> cacheNames) throws ServiceExecutionException;

  /**
   * A factory method for {@link CacheStatisticSampleEntityV2} objects.
   *
   * @param cacheManagerNames a {@code Set} of names for the {@link CacheManager}s that manage the {@link Cache}s whose
   *                          sampled statistics are to be represented by the returned resources
   * @param cacheNames        a {@code Set} of names for the {@link Cache}s whose sampled statistics are to be represented
   *                          by the returned resources
   * @param statNames         a {@code Set} of names for the sampled statistics to be represented by the returned resources
   * @return a {@code ResponseEntityV2} containing {@code CacheStatisticSampleEntity} objects
   */
  ResponseEntityV2 createCacheStatisticSampleEntity(Set<String> cacheManagerNames,
                                                                          Set<String> cacheNames,
                                                                          Set<String> statNames) throws ServiceExecutionException;
}
