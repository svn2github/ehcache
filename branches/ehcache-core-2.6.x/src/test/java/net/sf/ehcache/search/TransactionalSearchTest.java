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

package net.sf.ehcache.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.TransactionManager;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.search.Person.Gender;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import bitronix.tm.TransactionManagerServices;

@RunWith(Parameterized.class)
public class TransactionalSearchTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TransactionalMode.LOCAL}, {TransactionalMode.XA}, {TransactionalMode.XA_STRICT}
        });
    }

    @Before
    public void setupTransactionManager() {
        switch (txnMode) {
            case XA:
            case XA_STRICT:
                TransactionManagerServices.getConfiguration().setJournal("null").setGracefulShutdownInterval(0).setBackgroundRecoveryIntervalSeconds(1);
                txnManager = TransactionManagerServices.getTransactionManager();
                break;
            default:
                txnManager = null;
                break;
        }
    }

    @After
    public void shutdownTransactionManager() throws Exception {
        switch (txnMode) {
            case XA:
            case XA_STRICT:
                if (txnManager.getTransaction() != null) {
                    txnManager.rollback();
                }
                TransactionManagerServices.getTransactionManager().shutdown();
                txnManager = null;
                break;
            default:
                txnManager = null;
                break;
        }
    }

    private void beginTransaction(CacheManager manager) throws Exception {
        switch (txnMode) {
            case XA:
            case XA_STRICT:
                txnManager.begin();
                break;
            case LOCAL:
                manager.getTransactionController().begin();
                break;
        }
    }

    private void commitTransaction(CacheManager manager) throws Exception {
        switch (txnMode) {
            case XA:
            case XA_STRICT:
                txnManager.commit();
                break;
            case LOCAL:
                manager.getTransactionController().commit();
                break;
        }
    }

    private final TransactionalMode txnMode;

    private TransactionManager txnManager;

    public TransactionalSearchTest(TransactionalMode txnMode) {
        this.txnMode = txnMode;
    }

    private CacheConfiguration getBaseCacheConfiguration() {
        return new CacheConfiguration("jta-search", 0).transactionalMode(txnMode);
    }

    @Test
    public void testExpressionAttributeExtractorCache() throws Exception {
        CacheConfiguration config = getBaseCacheConfiguration();
        config.searchable(new Searchable().
                searchAttribute(new SearchAttribute().name("age").expression("value.getAge()")).
                searchAttribute(new SearchAttribute().name("gender").expression("value.getGender()")).
                searchAttribute(new SearchAttribute().name("name").expression("value.getName()")));
        testCacheWithConfiguration(config);
    }

    @Test
    public void testCustomAttributeExtractorCache() throws Exception {
        CacheConfiguration config = getBaseCacheConfiguration();
        config.searchable(new Searchable().
                searchAttribute(new SearchAttribute().name("age").className("net.sf.ehcache.search.TestAttributeExtractor")).
                searchAttribute(new SearchAttribute().name("gender").expression("value.getGender()")).
                searchAttribute(new SearchAttribute().name("name").expression("value.getName()")));
        testCacheWithConfiguration(config);
    }

    @Test
    public void testBeanAttributeExtractorCache() throws Exception {
        CacheConfiguration config = getBaseCacheConfiguration();
        config.searchable(new Searchable().
                searchAttribute(new SearchAttribute().name("age")).
                searchAttribute(new SearchAttribute().name("gender")).
                searchAttribute(new SearchAttribute().name("name")));
        testCacheWithConfiguration(config);
    }

    private void testCacheWithConfiguration(CacheConfiguration config) throws Exception {
        CacheManager cacheManager = new CacheManager();
        Ehcache cache = new Cache(config);
        cacheManager.addCache(cache);

        assertTrue(cache.isSearchable());

        beginTransaction(cacheManager);
        try {
            //This data should appear in the search results
            SearchTestUtil.populateData(cache);
        } finally {
            commitTransaction(cacheManager);
        }

        beginTransaction(cacheManager);
        try {
            //This data shouldn't appear in the search results
            cache.put(new Element(-1, new Person("Chris Dennis", 29, Gender.MALE)));
            cache.put(new Element(-2, new Person("Cassie (Dog)", 1, Gender.FEMALE)));
            basicQueries(cache);
        } finally {
            commitTransaction(cacheManager);
        }
        cacheManager.shutdown();
    }

    private void basicQueries(Ehcache cache) {
        Query query;
        Attribute<Integer> age = cache.getSearchAttribute("age");

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(age.ne(35));
        query.end();
        verify(cache, query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").lt(30));
        query.end();
        query.execute();
        verify(cache, query, 2);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").le(30));
        query.end();
        query.execute();
        verify(cache, query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").in(new HashSet(Arrays.asList(23, 35))));
        query.end();
        query.execute();
        verify(cache, query, 1, 2, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").gt(30));
        query.end();
        query.execute();
        verify(cache, query, 1, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").between(23, 35, true, false));
        query.end();
        query.execute();
        verify(cache, query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").ge(30));
        query.end();
        query.execute();
        verify(cache, query, 1, 3, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").eq(35).or(cache.getSearchAttribute("gender").eq(Gender.FEMALE)));
        query.end();
        verify(cache, query, 1, 2, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").eq(35).and(cache.getSearchAttribute("gender").eq(Gender.MALE)));
        query.end();
        verify(cache, query, 1, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").eq(35).and(cache.getSearchAttribute("gender").eq(Gender.FEMALE)));
        query.end();
        verify(cache, query);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("gender").eq(Gender.MALE).not());
        query.end();
        verify(cache, query, 2);

        try {
            cache.getSearchAttribute("DOES_NOT_EXIST_PLEASE_DO_NOT_CREATE_ME");
            fail();
        } catch (CacheException ce) {
            // expected
        }
    }

    private void verify(Ehcache cache, Query query, Integer... expectedKeys) {
        Results results = query.execute();
        assertEquals(expectedKeys.length, results.size());
        assertTrue(results.hasKeys());
        assertFalse(results.hasAttributes());

        Set<Integer> keys = new HashSet<Integer>(Arrays.asList(expectedKeys));

        for (Result result : results.all()) {
            int key = (Integer) result.getKey();
            if (!keys.remove(key)) {
                throw new AssertionError("unexpected key: " + key);
            }
        }
    }
}
