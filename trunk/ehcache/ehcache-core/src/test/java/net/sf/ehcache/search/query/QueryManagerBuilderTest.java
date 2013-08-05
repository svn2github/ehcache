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

package net.sf.ehcache.search.query;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.search.Query;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.sf.ehcache.search.query.QueryManagerBuilder.newQueryManagerBuilder;

public class QueryManagerBuilderTest {

    @Test
    public void testDefaultsToProperDefaultClass() {
        try {
            newQueryManagerBuilder();
        } catch (CacheException e) {
            Assert.assertTrue(e.getCause().getClass() == ClassNotFoundException.class);
        }
    }

    @Test
    public void testDoesNotShareStateAmongBuilds() {
        List<Ehcache> caches_for_build_1 = new ArrayList<Ehcache>();
        CacheManager cm = buildCacheManagerAndAddCaches();
        for (String cacheName : cm.getCacheNames()) {
            caches_for_build_1.add(cm.getCache(cacheName));
        }

        final QueryManagerBuilder queryManagerBuilder1 = new QueryManagerBuilder(MockQueryManager.class);
        queryManagerBuilder1.addAllCachesCurrentlyIn(cm);
        final QueryManager build1 = queryManagerBuilder1.build();

        // assert that build1 added all caches of cm
        for (Ehcache ehcache : caches_for_build_1) {
            Assert.assertTrue(((MockQueryManager)build1).queryManagerEhcaches.contains(ehcache));
        }

        List<Ehcache> caches_for_build_2 = addMoreCachesToCacheManager(cm);

        final QueryManagerBuilder queryManagerBuilder2 = new QueryManagerBuilder(MockQueryManager.class);
        for (Ehcache ehcache : caches_for_build_2) {
            queryManagerBuilder2.addCache(ehcache);
        }

        final QueryManager build2 = queryManagerBuilder2.build();

        // assert that build2 contains all caches for its build
        for (Ehcache ehcache : caches_for_build_2) {
            Assert.assertTrue("build2 doesn't contain " + ehcache.getName(),
                ((MockQueryManager)build2).queryManagerEhcaches.contains(ehcache));
        }

        // assert that build1 still contains its caches
        for (Ehcache ehcache : caches_for_build_1) {
            Assert.assertTrue("build2 doesn't contain " + ehcache.getName(),
                ((MockQueryManager)build1).queryManagerEhcaches.contains(ehcache));
        }


        // assert that build1 doesn't have the caches from build2
        for (Ehcache ehcache : caches_for_build_2) {
            Assert.assertFalse("build1 shouldn't contain the cache " + ehcache.getName(),
                ((MockQueryManager)build1).queryManagerEhcaches.contains(ehcache));
        }


        // assert that build2 doesn't have the caches from build1
        for (Ehcache ehcache : caches_for_build_1) {
            Assert.assertFalse("build2 shouldn't contain the cache " + ehcache.getName(),
                ((MockQueryManager)build2).queryManagerEhcaches.contains(ehcache));
        }
    }

    private CacheManager buildCacheManagerAndAddCaches() {
        Configuration cmConfig = new Configuration().name("myCacheManager");
        CacheManager cm = new CacheManager(cmConfig);
        Ehcache ehcache;
        for (int i = 0; i < 5; i++) {
            ehcache = new Cache(new CacheConfiguration().name("foo-" + i).maxEntriesLocalHeap(10));
            cm.addCache(ehcache);
        }
        return cm;
    }

    private List<Ehcache> addMoreCachesToCacheManager(CacheManager cm) {
        List<Ehcache> ehcacheList = new ArrayList<Ehcache>();
        Ehcache ehcache;
        for (int i = 0; i < 5; i++) {
            ehcache = new Cache(new CacheConfiguration().name("bar-" + i).maxEntriesLocalHeap(10));
            cm.addCache(ehcache);
            ehcacheList.add(ehcache);
        }
        return ehcacheList;
    }

}


class MockQueryManager implements QueryManager {

    final Collection<Ehcache> queryManagerEhcaches;

    public MockQueryManager(Collection<Ehcache> ehcacheList) {
        this.queryManagerEhcaches = ehcacheList;
    }

    @Override
    public Query createQuery(final String statement) throws CacheException {
        throw new UnsupportedOperationException("Implement me!");
    }
}
