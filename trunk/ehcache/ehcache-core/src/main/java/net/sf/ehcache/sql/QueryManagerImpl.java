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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.search.Query;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Implementation of the {@link net.sf.ehcache.sql.QueryManager Query Manager} interface.
 *
 * @author dkumar
 */

public class QueryManagerImpl implements QueryManager {

  private final Class queryParserClazz;
  private final Object queryParserInstance;
  private final QueryManagerBuilder queryManagerBuilder;

  /**
   *
   * Package-local constructor for creating the Query Manager
   *
   * @param queryManagerBuilder builder to create the Query Manager
   * @throws Exception 
   */
  QueryManagerImpl(QueryManagerBuilder queryManagerBuilder) throws Exception {
    this.queryParserClazz = Class.forName("net.sf.ehcache.search.parser.QueryParserImpl");
    this.queryParserInstance = queryParserClazz.newInstance();
    this.queryManagerBuilder = queryManagerBuilder;
  }

  @Override
  public Query createQuery(String statement) throws CacheException {
    Method searchCacheNameMethod;
    String targetCacheName;
    try {
      searchCacheNameMethod = queryParserClazz.getDeclaredMethod("extractSearchCacheName", String.class);
      targetCacheName = (String)searchCacheNameMethod.invoke(queryParserInstance, statement);
    } catch (NoSuchMethodException n) {
      throw new CacheException(n);
    } catch (InvocationTargetException i) {
      throw new CacheException(i);
    } catch (IllegalAccessException a) {
      throw new CacheException(a);
    }
    Cache targetCache = queryManagerBuilder.getCache(targetCacheName);

    Method createQueryMethod;
    Query query;
    try {
      createQueryMethod = queryParserClazz.getDeclaredMethod("createQuery", Cache.class, String.class);
      query = (Query)createQueryMethod.invoke(queryParserInstance, targetCache,  statement);
    } catch (NoSuchMethodException n) {
      throw new CacheException(n);
    } catch (InvocationTargetException i) {
      throw new CacheException(i);
    } catch (IllegalAccessException a) {
      throw new CacheException(a);
    }
    return query;
  }
}
