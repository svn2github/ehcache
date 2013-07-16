/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.sql;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link net.sf.ehcache.sql.QueryManager Query Manager} builder providing methods to add caches which
 * can be queried.
 * The Query Manager instance returned by this builder can be used to execute
 * search queries expressed as Big Memory Structured Query Language (BMSQL) statements.
 *
 * @author dkumar
 */
public final class QueryManagerBuilder {

  private final List<Cache> caches = new ArrayList<Cache>();

  private QueryManagerBuilder() { }

  /**
   * Creates a new {@link net.sf.ehcache.sql.QueryManager Query Manager} builder.
   * @return this for the builder pattern
   */
  public static QueryManagerBuilder newQueryManagerBuilder() {
    return new QueryManagerBuilder();
  }

  /**
   * Adds a {@link net.sf.ehcache.Cache Cache} to the internal state and allows it to be used as a target in a
   * BMSQL statement's FROM clause.
   *
   * @param cache a {@link net.sf.ehcache.Cache cache} instance to be added
   * @return this for the builder pattern
   */
  public QueryManagerBuilder addCache(Cache cache) {
    this.caches.add(cache);
    return this;
  }

  /**
   * Adds all {@link net.sf.ehcache.Cache Caches} present in a {@link net.sf.ehcache.CacheManager Cache Manager} to
   * the internal state of the query manager.
   * The added caches can be used in a BMSQL statement's FROM clause.
   *
   * @param cacheManager a {@link net.sf.ehcache.CacheManager Cache Manager} whose
   * {@link net.sf.ehcache.Cache caches} need to be added
   * @return this for the builder pattern
   */
  public QueryManagerBuilder addAllCachesCurrentlyIn(CacheManager cacheManager) {
    for (String s : cacheManager.getCacheNames()) {
      final Cache cache = cacheManager.getCache(s);
      if (cache != null) {
        this.caches.add(cache);
      }
    }
    return this;
  }

  /**
   * Returns a new {@link net.sf.ehcache.sql.QueryManager Query Manager} using the options supplied during the building
   * process.
   * @return the fully constructed {@link net.sf.ehcache.sql.QueryManager Query Manager}
   */
  public QueryManager build() {
    try {
      return new QueryManagerImpl(this);
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }


  /**
   * Returns a cache instance with a given name
   *
   * @param cacheName name of the cache
   * @return cache instance
   * @throws CacheException if the cache could not be found, or if more than one cache exists with the same name
   */
  Cache getCache(String cacheName) throws CacheException {
    Cache cache = null;
    int numCachesFound = 0;
    for (Cache c : caches) {
      if (c.getName().equals(cacheName)) {
        numCachesFound++;
        cache = c;
      }
    }

    if (numCachesFound == 0) {
      throw new CacheException("The cache specified with the FROM clause could not be found.");
    } else if (numCachesFound > 1) {
      throw new CacheException("More than one cache with the same name was found");
    } else {
      return cache;
    }
  }
}
