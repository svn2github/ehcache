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
package net.sf.ehcache.search.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.ehcache.search.parser.EhcacheSearchParser;
import net.sf.ehcache.search.parser.ParseException;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.query.QueryManager;

/**
 * Implementation of the QueryParser interface of ehcache-core.
 */
public class QueryManagerImpl implements QueryManager {

  private final List<Ehcache> caches = new ArrayList<Ehcache>();

  public QueryManagerImpl(Collection<Ehcache> ehcaches) {
    this.caches.addAll(ehcaches);
  }

  Results search(Ehcache cache, String statement) throws SearchException {
    return createQuery(cache, statement).end().execute();
  }

  private Query createQuery(Ehcache cache, String statement) throws SearchException {
    EhcacheSearchParser parser=new EhcacheSearchParser(new StringReader(statement));
    ParseModel model;
    try {
      model = parser.QueryStatement();
    } catch (ParseException p) {
      throw new SearchException(p);
    }
    return model.getQuery(cache);
  }

  public Query createQuery(String statement) throws SearchException {
    String cacheName = extractSearchCacheName(statement);
    if (cacheName == null) {
      throw new SearchException("Please specify the cache's name with the FROM clause.");
    } else {
      return createQuery(getCache(cacheName), statement);
    }
  }

  String extractSearchCacheName(String statement) throws SearchException {
    EhcacheSearchParser parser=new EhcacheSearchParser(new StringReader(statement));
    ParseModel model = null;
    try {
      model = parser.QueryStatement();
    } catch (ParseException p) {
      throw new SearchException(p);
    }
    return model.getCacheName();
  }

  String extractCacheManagerName(String statement) throws SearchException {
    // TODO: implement this method
    return null;
  }

  private Ehcache getCache(String cacheName) throws CacheException {
    Ehcache cache = null;
    int numCachesFound = 0;
    for (Ehcache c : caches) {
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
