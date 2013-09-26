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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Person.Gender;
import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;
import net.sf.ehcache.search.impl.GroupedResultImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class CopyOnRWSearchTest {

    private final boolean copyOnRead;
    private final boolean copyOnWrite;

    @Parameters(name = "copyOnRead:{0}, copyOnWrite:{1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { true, false }, { false, true }, { true, true } };
        return Arrays.asList(data);
    }

    public CopyOnRWSearchTest(boolean copyOnRead, boolean copyOnWrite) {
        this.copyOnRead = copyOnRead;
        this.copyOnWrite = copyOnWrite;
    }

    private CacheConfiguration getBaseCacheConfiguration() {
        return new CacheConfiguration("copy-search", 0).copyOnRead(copyOnRead).copyOnWrite(copyOnWrite);
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

    @Test
    public void testBeanAttributeExtractorWithTypeCache() throws Exception {
        CacheConfiguration config = getBaseCacheConfiguration();
        config.searchable(new Searchable().
                searchAttribute(new SearchAttribute().name("age").type("int")).
                searchAttribute(new SearchAttribute().name("gender").type(Gender.class.getName())).
                searchAttribute(new SearchAttribute().name("name").type("String")));
        testCacheWithConfiguration(config);
    }

    @Test
    public void testDynamicAttributeExtractorCache() throws Exception {
        CacheConfiguration config = getBaseCacheConfiguration();
        Searchable searchable = new Searchable();
        searchable.allowDynamicIndexing(true);
        config.searchable(searchable);
        config.setDynamicAttributesExtractor(new DynamicAttributesExtractor() {
            @Override
            public Map<String, Object> attributesFor(Element element) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("age", ((Person)element.getObjectValue()).getAge());
                map.put("gender", ((Person)element.getObjectValue()).getGender());
                map.put("name", ((Person)element.getObjectValue()).getName());
                return map;
            }
        });
        testCacheWithConfiguration(config);
    }

    private void testCacheWithConfiguration(CacheConfiguration config) throws Exception {
        CacheManager cacheManager = new CacheManager();
        try {
            Ehcache cache = new Cache(config);
            cacheManager.addCache(cache);

            assertTrue(cache.isSearchable());

            //This data should appear in the search results
            SearchTestUtil.populateData(cache);

            basicQueries(cache);

            valueQuery(cache);
        } finally {
            cacheManager.shutdown();
        }
    }

    private void valueQuery(Ehcache cache) {
        Query query = cache.createQuery();
        query.includeValues();
        query.addCriteria(cache.getSearchAttribute("gender").eq(Gender.FEMALE));
        query.end();

        Results results = query.execute();
        assertThat(results.all().get(0).getValue(), instanceOf(Person.class));
    }

    private void basicQueries(Ehcache cache) {
        Query query;
        Attribute<Integer> age = cache.getSearchAttribute("age");

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(age.ne(35));
        query.end();
        verify(query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").lt(30));
        query.end();
        query.execute();
        verify(query, 2);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").le(30));
        query.end();
        query.execute();
        verify(query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").in(new HashSet<Integer>(Arrays.asList(23, 35))));
        query.end();
        query.execute();
        verify(query, 1, 2, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").gt(30));
        query.end();
        query.execute();
        verify(query, 1, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").between(23, 35, true, false));
        query.end();
        query.execute();
        verify(query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").ge(30));
        query.end();
        query.execute();
        verify(query, 1, 3, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").eq(35).or(cache.getSearchAttribute("gender").eq(Gender.FEMALE)));
        query.end();
        verify(query, 1, 2, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").eq(35).and(cache.getSearchAttribute("gender").eq(Gender.MALE)));
        query.end();
        verify(query, 1, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("age").eq(35).and(cache.getSearchAttribute("gender").eq(Gender.FEMALE)));
        query.end();
        verify(query);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("gender").eq(Gender.MALE).not());
        query.end();
        verify(query, 2);

        try {
            cache.getSearchAttribute("DOES_NOT_EXIST_PLEASE_DO_NOT_CREATE_ME");
            fail();
        } catch (CacheException ce) {
            // expected
        }
    }

    private void verify(Query query, Integer... expectedKeys) {
        Results results = query.execute();
        assertEquals(expectedKeys.length, results.size());
        if (expectedKeys.length == 0) {
            assertFalse(results.hasKeys());
        } else {
            assertTrue(results.hasKeys());
        }
        assertFalse(results.hasAttributes());

        Set<Integer> keys = new HashSet<Integer>(Arrays.asList(expectedKeys));

        for (Result result : results.all()) {
            int key = (Integer) result.getKey();
            if (!keys.remove(key)) {
                throw new AssertionError("unexpected key: " + key);
            }
        }
    }

    @Test
    public void testBasicGroupBy() throws Exception {
        CacheConfiguration config = getBaseCacheConfiguration();
        config.searchable(new Searchable().
                searchAttribute(new SearchAttribute().name("age")).
                searchAttribute(new SearchAttribute().name("gender")).
                searchAttribute(new SearchAttribute().name("name")).
                searchAttribute(new SearchAttribute().name("department")));
        CacheManager cacheManager = new CacheManager();

        try {
            Ehcache cache = new Cache(config);
            cacheManager.addCache(cache);
            assertTrue(cache.isSearchable());

            int numOfDepts = 10;
            int numOfMalesPerDept = 100;
            int numOfFemalesPerDept = 100;

            for (int i = 0; i < numOfDepts; i++) {
                for (int j = 0; j < numOfMalesPerDept; j++) {
                    cache.put(new Element("male" + i + "-" + j, new Person("male" + j, j, Gender.MALE, "department" + i)));
                }

                for (int j = 0; j < numOfFemalesPerDept; j++) {
                    cache.put(new Element("female" + i + "-" + j, new Person("female" + j, j, Gender.FEMALE, "department" + i)));
                }
            }


            Query query;
            Results results;

            query = cache.createQuery();
            query.includeAttribute(cache.getSearchAttribute("department"));
            query.includeAttribute(cache.getSearchAttribute("gender"));
            query.includeAggregator(cache.getSearchAttribute("age").sum());
            query.includeAggregator(cache.getSearchAttribute("age").min());
            query.includeAggregator(cache.getSearchAttribute("age").max());
            query.addGroupBy(cache.getSearchAttribute("department"));
            query.addOrderBy(cache.getSearchAttribute("department"), Direction.DESCENDING);
            query.addOrderBy(cache.getSearchAttribute("gender"), Direction.ASCENDING);
            query.addGroupBy(cache.getSearchAttribute("gender"));
            query.end();

            results = query.execute();

            assertEquals(numOfDepts * 2, results.size());

            int i = 1;
            for (Iterator<Result> iter = results.all().iterator(); iter.hasNext();) {
                Result maleResult = iter.next();

                System.out.println("XXXXXXXXX: " + maleResult);
                assertTrue(maleResult instanceof GroupedResultImpl);
                assertEquals("department" + (numOfDepts - i), maleResult.getAttribute(cache.getSearchAttribute("department")));
                assertEquals(Gender.MALE, maleResult.getAttribute(cache.getSearchAttribute("gender")));

                Map<String, Object> groupByValues = ((GroupedResultImpl) maleResult).getGroupByValues();
                assertEquals(2, groupByValues.size());
                assertEquals("department" + (numOfDepts - i), groupByValues.get("department"));
                assertEquals(Gender.MALE, groupByValues.get("gender"));

                List aggregateResults = maleResult.getAggregatorResults();
                assertEquals(3, aggregateResults.size());
                assertEquals(numOfMalesPerDept * (numOfMalesPerDept - 1) / 2, ((Long) aggregateResults.get(0)).intValue());
                assertEquals(0, ((Integer) aggregateResults.get(1)).intValue());
                assertEquals(numOfMalesPerDept - 1, ((Integer) aggregateResults.get(2)).intValue());

                Result femaleResult = iter.next();
                System.out.println("XXXXXXXXX: " + femaleResult);

                assertEquals("department" + (numOfDepts - i), femaleResult.getAttribute(cache.getSearchAttribute("department")));
                assertEquals(Gender.FEMALE, femaleResult.getAttribute(cache.getSearchAttribute("gender")));

                assertTrue(femaleResult instanceof GroupedResultImpl);
                groupByValues = ((GroupedResultImpl) femaleResult).getGroupByValues();
                assertEquals(2, groupByValues.size());
                assertEquals("department" + (numOfDepts - i), groupByValues.get("department"));
                assertEquals(Gender.FEMALE, groupByValues.get("gender"));

                aggregateResults = femaleResult.getAggregatorResults();
                assertEquals(3, aggregateResults.size());
                assertEquals(numOfFemalesPerDept * (numOfFemalesPerDept - 1) / 2, ((Long) aggregateResults.get(0)).intValue());
                assertEquals(0, ((Integer) aggregateResults.get(1)).intValue());
                assertEquals(numOfFemalesPerDept - 1, ((Integer) aggregateResults.get(2)).intValue());

                i++;
            }
        } finally {
            cacheManager.shutdown();
        }


    }
}
