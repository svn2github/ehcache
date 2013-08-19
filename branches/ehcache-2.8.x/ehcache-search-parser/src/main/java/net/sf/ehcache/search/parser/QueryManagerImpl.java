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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.query.QueryManager;

/**
 * Implementation of the QueryParser interface of ehcache-core.
 */
public class QueryManagerImpl implements QueryManager {

    private final Map<CacheManager, List<Ehcache>> cacheManagerEhcacheMap = new HashMap<CacheManager, List<Ehcache>>();

    public QueryManagerImpl(Collection<Ehcache> ehcaches) {
        CacheManager cm;
        for (Ehcache ehcache : ehcaches) {
            cm = ehcache.getCacheManager();
            if (cacheManagerEhcacheMap.containsKey(cm)) {
                cacheManagerEhcacheMap.get(cm).add(ehcache);
            } else {
                List<Ehcache> ehcacheList = new ArrayList<Ehcache>();
                ehcacheList.add(ehcache);
                cacheManagerEhcacheMap.put(cm, ehcacheList);
            }
        }
    }

    Results search(Ehcache cache, String statement) throws SearchException {
        return createQuery(cache, statement).end().execute();
    }

    @Override
    public Query createQuery(String statement) throws SearchException {
        Map<String, String> cacheManagerCacheNameMap = extractSearchCacheName(statement);
        String cacheManagerName = cacheManagerCacheNameMap.values().iterator().next();
        String cacheName = cacheManagerCacheNameMap.keySet().iterator().next();
        if (cacheManagerCacheNameMap.size() == 0) {
            throw new SearchException("Please specify the cache's name with the FROM clause.");
        } else {
            return createQuery(getCache(cacheName, cacheManagerName), statement);
        }
    }

    // returns a map of cache name and cache manager name
    Map<String, String> extractSearchCacheName(String statement) throws SearchException {
        EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(statement));
        ParseModel model = null;
        try {
            model = parser.QueryStatement();
        } catch (ParseException p) {
            throw new SearchException(p);
        }
        Map<String, String> retMap = new HashMap<String, String>();
        String cacheName = model.getCacheName();
        String cacheManagerName = model.getCacheManagerName();
        retMap.put(cacheName, cacheManagerName);
        return retMap;
    }

    private Query createQuery(Ehcache cache, String statement) throws SearchException {
        EhcacheSearchParser parser = new EhcacheSearchParser(new StringReader(statement));
        ParseModel model;
        try {
            model = parser.QueryStatement();
        } catch (ParseException p) {
            throw new SearchException(p);
        }
        return model.getQuery(cache);
    }

    private Ehcache getCache(String cacheName, String cacheManagerName) throws CacheException {
        Ehcache cache = null;
        List<Ehcache> foundCaches = new ArrayList<Ehcache>();
        int numCachesFound = 0;

        Iterator<Ehcache> ehcacheIterator;
        for (List<Ehcache> ehcacheList : cacheManagerEhcacheMap.values()) {
            ehcacheIterator = ehcacheList.iterator();
            Ehcache c;
            while (ehcacheIterator.hasNext()) {
                c = ehcacheIterator.next();
                if (c.getName().equals(cacheName)) {
                    numCachesFound++;
                    cache = c;
                    foundCaches.add(c);
                }
            }
        }

        if (numCachesFound == 0) {
            throw new CacheException("The cache specified with the FROM clause could not be found.");
        } else if (numCachesFound > 1 && cacheManagerName == null) {
            throw new CacheException("More than one cache with the same name was found");
        } else {
            if (cacheManagerName == null) {
                return cache;
            } else {
                for (Ehcache ehcache : foundCaches) {
                    if (ehcache.getCacheManager().getName().equals(cacheManagerName)) {
                        return ehcache;
                    }
                }
                throw new CacheException("Cache with the name " + cacheName +
                                         " was not found in " + cache.getCacheManager().getName()
                                         + " , Expected cache manager name = " + cacheManagerName);
            }
        }
    }
}


