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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Person.Gender;
import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.AggregatorException;
import net.sf.ehcache.search.aggregator.AggregatorInstance;
import net.sf.ehcache.search.expression.Or;
import net.sf.ehcache.search.impl.GroupedResultImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BasicSearchTest {
    private CacheManager cacheManager;

    @Before
    public void setup() {
        this.cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
    }

    @After
    public void tearDown() {
        this.cacheManager.shutdown();
    }

    @Test
    public void testInvalidConfiguration() {
        try {
            new CacheManager(getClass().getResource("/ehcache-search-invalid-key.xml"));
            fail();
        } catch (CacheException ce) {
            // expected
        }

        try {
            new CacheManager(getClass().getResource("/ehcache-search-invalid-value.xml"));
            fail();
        } catch (CacheException ce) {
            // expected
        }
    }

    @Test
    public void testKeysValuesDisabled() {
        Cache cache = cacheManager.getCache("searchable-no-keys-values");

        CacheConfiguration config = cache.getCacheConfiguration();

        Searchable searchable = config.getSearchable();

        assertFalse(searchable.keys());
        assertFalse(searchable.values());

        try {
            cache.getSearchAttribute(Query.KEY.getAttributeName());
            fail();
        } catch (CacheException se) {
            // expected
            System.err.println(se.getMessage());
        }

        try {
            cache.getSearchAttribute(Query.VALUE.getAttributeName());
            fail();
        } catch (CacheException se) {
            // expected
            System.err.println(se.getMessage());
        }
    }

    @Test
    public void testNonSearchableCache() {
        Ehcache cache = cacheManager.getEhcache("not-searchable");
        assertFalse(cache.isSearchable());

        try {
            cache.createQuery();
            fail();
        } catch (CacheException e) {
            // expected
        }
    }

    @Test
    public void testDefaultSearchableCache() {
        Ehcache cache = cacheManager.getEhcache("default-searchable");
        assertTrue(cache.isSearchable());

        cache.put(new Element("key", new Object()));
        cache.put(new Element(new Object(), "value"));
        cache.put(new Element(new Object(), new Object()));
        cache.put(new Element(null, null));

        Query query;
        Results results;

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(Query.KEY.eq("key")).end();
        results = query.execute();
        assertEquals(1, results.size());
        assertEquals("key", results.all().iterator().next().getKey());

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(Query.VALUE.eq("value")).end();
        results = query.execute();
        assertEquals(1, results.size());
        Object key = results.all().iterator().next().getKey();
        assertEquals("value", cache.get(key).getObjectValue());
    }

    @Test
    public void testQueryBuilder() {
        Ehcache cache = cacheManager.getEhcache("cache1");

        Query query1 = cache.createQuery();
        Query query2 = cache.createQuery();

        // query instances should be unique
        assertFalse(query1 == query2);

        // null checks
        try {
            query1.addCriteria(null);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.addOrderBy(null, Direction.ASCENDING);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.addOrderBy(new Attribute("foo"), null);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.includeAggregator((Aggregator[]) null);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.includeAttribute((Attribute[]) null);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.includeAttribute(new Attribute[] {new Attribute("foo"), null});
            fail();
        } catch (NullPointerException npe) {
            // expected
        }

        // freeze query
        query1.end();

        try {
            query1.addCriteria(new Attribute("foo").le(35));
            fail();
        } catch (SearchException se) {
            // expected
        }
        try {
            query1.addOrderBy(new Attribute("foo"), Direction.ASCENDING);
            fail();
        } catch (SearchException se) {
            // expected
        }
        try {
            query1.includeAggregator(new Attribute("foo").max());
            fail();
        } catch (SearchException se) {
            // expected
        }
        try {
            query1.includeAttribute(new Attribute("foo"));
            fail();
        } catch (SearchException se) {
            // expected
        }
        try {
            query1.includeKeys();
            fail();
        } catch (SearchException se) {
            // expected
        }
        try {
            query1.maxResults(3);
            fail();
        } catch (SearchException se) {
            // expected
        }
    }

    @Test
    public void testRange() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Query query = cache.createQuery();
        query.includeKeys();
        query.end();

        Results results = query.execute();
        assertEquals(4, results.all().size());

        List<Integer> keys = new ArrayList<Integer>();
        for (int i = 0; i < 4; i++) {
            List<Result> range = results.range(i, 1);
            assertEquals(1, range.size());
            keys.add((Integer) range.get(0).getKey());
        }
        assertEquals(4, keys.size());

        for (int i = 0; i < 4; i++) {
            assertEquals(0, results.range(i, 0).size());
        }

        assertEquals(0, results.range(0, 0).size());
        assertEquals(1, results.range(0, 1).size());
        assertEquals(2, results.range(0, 2).size());
        assertEquals(3, results.range(0, 3).size());
        assertEquals(4, results.range(0, 4).size());
        assertEquals(4, results.range(0, 5).size());
        assertEquals(4, results.range(0, Integer.MAX_VALUE).size());

        try {
            results.range(-1, 1);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            results.range(0, -1);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testBasic() {
        // uses expression attribute extractors
        basicQueries(cacheManager.getEhcache("cache1"));

        // uses a "custom" attribute extractor too
        basicQueries(cacheManager.getEhcache("cache2"));

        // uses bean attributes
        basicQueries(cacheManager.getEhcache("bean-attributes"));
    }

    @Test
    public void testCustomAggregator() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");

        Query query = cache.createQuery();
        query.includeAggregator(new Aggregator() {
            public AggregatorInstance<Integer> createInstance() {
                return new AggregatorInstance<Integer>() {

                    private int doubledSum;

                    public void accept(Object input) throws AggregatorException {
                        if (doubledSum == 0) {
                            doubledSum = (2 * (Integer) input);
                        } else {
                            doubledSum += (2 * (Integer) input);
                        }
                    }

                    public AggregatorInstance<Integer> createClone() {
                        return this;
                    }

                    public Integer aggregateResult() {
                        return doubledSum;
                    }

                    public Attribute<?> getAttribute() {
                        return new Attribute("age");
                    }
                };
            }
        });
        query.end();

        Results results = query.execute();
        assertEquals(1, results.size());
        for (Result result : results.all()) {
            assertEquals(246, result.getAggregatorResults().get(0));
        }
    }

    @Test
    public void testBuiltinFunctions() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");

        {
            Query query = cache.createQuery();
            query.includeAggregator(age.count());
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertEquals(1, results.size());
            for (Result result : results.all()) {
                System.err.println(result); // exercise toString()
                assertEquals(4, result.getAggregatorResults().get(0));
            }
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(age.max());
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertEquals(1, results.size());
            for (Result result : results.all()) {
                assertEquals(35, result.getAggregatorResults().get(0));
            }
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(age.min());
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertEquals(1, results.size());
            for (Result result : results.all()) {
                assertEquals(23, result.getAggregatorResults().get(0));
            }
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(age.sum());
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertEquals(1, results.size());
            for (Result result : results.all()) {
                assertEquals(123L, result.getAggregatorResults().get(0));
            }
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(age.average());
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertEquals(1, results.size());
            for (Result result : results.all()) {
                assertEquals(30.75F, result.getAggregatorResults().get(0));
            }
        }

        {
            // multiple aggregators
            Query query = cache.createQuery();
            query.includeAggregator(age.min());
            query.includeAggregator(age.max());
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertEquals(1, results.size());
            for (Result result : results.all()) {
                assertEquals(23, result.getAggregatorResults().get(0));
                assertEquals(35, result.getAggregatorResults().get(1));
            }
        }

        {
            // use criteria with an aggregator
            Query query = cache.createQuery();
            query.includeAggregator(age.average());
            query.addCriteria(age.between(0, 32));
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertEquals(1, results.size());
            for (Result result : results.all()) {
                assertEquals(26.5F, result.getAggregatorResults().get(0));
            }
        }

        {
            // includeKeys in addition to an aggregator
            Query query = cache.createQuery();
            query.includeKeys();
            query.includeAggregator(age.average());
            query.addCriteria(age.between(0, 32));
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertTrue(results.hasKeys());
            assertEquals(2, results.size());
            for (Result result : results.all()) {
                assertEquals(26.5F, result.getAggregatorResults().get(0));
            }

            verify(cache, query, 2, 4);
        }

        {
            // execute query twice
            Query query = cache.createQuery();
            query.includeAggregator(age.count());
            query.end();

            Results results = query.execute();
            assertTrue(results.hasAggregators());
            assertFalse(results.hasKeys());
            for (Result result : results.all()) {
                assertEquals(4, result.getAggregatorResults().get(0));
            }

            results = query.execute();
            assertTrue(results.hasAggregators());
            assertFalse(results.hasKeys());
            for (Result result : results.all()) {
                assertEquals(4, result.getAggregatorResults().get(0));
            }
        }

    }

    @Test
    public void testMaxResults() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<Person.Gender> gender = cache.getSearchAttribute("gender");

        Query query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(age.ne(35));
        query.maxResults(1);
        query.end();

        Results results = query.execute();
        assertEquals(1, results.size());
        for (Result result : results.all()) {
            switch ((Integer) result.getKey()) {
                case 2:
                case 4: {
                    break;
                }
                default: {
                    throw new AssertionError(result.getKey());
                }
            }
        }

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(age.ne(35));
        query.maxResults(0);
        query.end();

        results = query.execute();
        assertEquals(0, results.size());

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(age.ne(35));
        query.maxResults(2);
        query.end();

        results = query.execute();
        assertEquals(2, results.size());

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(age.ne(35));
        query.maxResults(2);
        query.end();

        results = query.execute();
        assertEquals(2, results.size());

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(age.ne(35));
        query.maxResults(-1);
        query.end();

        results = query.execute();
        assertEquals(2, results.size());

    }

    @Test
    public void testAttributeQuery() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<Person.Gender> gender = cache.getSearchAttribute("gender");

        Query query = cache.createQuery();
        // not including keys
        query.addCriteria(age.ne(35));
        query.includeAttribute(age, gender);
        query.end();

        Results results = query.execute();
        assertFalse(results.hasKeys());
        assertFalse(results.hasAggregators());
        assertTrue(results.hasAttributes());

        for (Result result : results.all()) {
            System.err.println(result.toString()); // exercise toString()

            try {
                result.getKey();
                fail();
            } catch (SearchException se) {
                // expected
            }

            try {
                result.getKey();
                fail();
            } catch (SearchException se) {
                // expected
            }

            int ageAttr = result.getAttribute(age);
            if (ageAttr == 23) {
                assertEquals(Gender.FEMALE, result.getAttribute(gender));
            } else if (ageAttr == 30) {
                assertEquals(Gender.MALE, result.getAttribute(gender));
            } else {
                throw new AssertionError("unexpected age: " + ageAttr);
            }

            try {
                result.getAttribute(new Attribute("does-not-exist"));
                fail();
            } catch (SearchException se) {
                // expected
            }
        }

    }

    private void basicQueries(Ehcache cache) {
        SearchTestUtil.populateData(cache);

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
        query.addCriteria(cache.getSearchAttribute("age").eq(35));
        query.addCriteria(cache.getSearchAttribute("gender").eq(Gender.FEMALE));
        query.end();
        verify(cache, query);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("gender").eq(Gender.MALE).not());
        query.end();
        verify(cache, query, 2);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("name").eq("Tim Eck"));
        query.addCriteria(cache.getSearchAttribute("gender").eq(Gender.MALE));
        query.addCriteria(cache.getSearchAttribute("age").eq(35));
        query.end();
        verify(cache, query, 1);

        query = cache.createQuery();
        query.includeKeys();
        Attribute name = cache.getSearchAttribute("name");
        query.addCriteria(name.eq("Tim Eck").or(name.eq("Ari Zilka")).or(name.eq("Nabib El-Rahman")));
        query.end();
        verify(cache, query, 1, 3, 4);

        try {
            cache.getSearchAttribute("DOES_NOT_EXIST_PLEASE_DO_NOT_CREATE_ME");
            fail();
        } catch (CacheException ce) {
            // expected
        }
    }

    @Test
    public void testOrdering() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<String> name = cache.getSearchAttribute("name");

        Query query;

        query = cache.createQuery();
        query.includeKeys();
        // no critera -- select all elements
        query.addOrderBy(age, Direction.DESCENDING);
        query.addOrderBy(name, Direction.ASCENDING);
        query.end();

        verifyOrdered(cache, query, 3, 1, 4, 2);

        query = cache.createQuery();
        query.includeKeys();
        // no critera -- select all elements
        query.addOrderBy(age, Direction.DESCENDING);
        query.addOrderBy(name, Direction.ASCENDING);
        query.maxResults(2);
        query.end();

        verifyOrdered(cache, query, 3, 1);
    }

    @Test
    public void testILike() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<String> name = cache.getSearchAttribute("name");

        Query query;

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(new Or(name.ilike("tim*"), name.ilike("ari*")));
        query.end();

        verify(cache, query, 3, 1);

        cache.removeAll();
        cache.put(new Element(1, new Person("Test \\ Bob * ?", 35, Gender.MALE)));
        cache.put(new Element(2, new Person("(..Test", 35, Gender.MALE)));
        cache.put(new Element(3, new Person("lowercase", 35, Gender.MALE)));
        cache.put(new Element(4, new Person("UPPERCASE", 35, Gender.MALE)));
        cache.put(new Element(5, new Person("MiXeD", 35, Gender.MALE)));
        cache.put(new Element(6, new Person("Hello there\nI am on a newline\nMe too\n", 999, Gender.MALE)));

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("Test \\\\ Bob \\* \\?"));
        query.end();

        verify(cache, query, 1);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("*Test*"));
        query.end();

        verify(cache, query, 1, 2);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("Test*\\?"));
        query.end();

        verify(cache, query, 1);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("(..*"));
        query.end();

        verify(cache, query, 2);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("Lowercase"));
        query.end();

        verify(cache, query, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("LOWER*"));
        query.end();

        verify(cache, query, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("uppercase"));
        query.end();

        verify(cache, query, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("mixed"));
        query.end();

        verify(cache, query, 5);

        query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(name.ilike("*am on a*"));
        query.end();

        verify(cache, query, 6);
    }

    @Test
    public void testTypeChecking() {
        CacheConfiguration config = new CacheConfiguration("test", 0);
        config.setOverflowToDisk(false);
        config.diskPersistent(false);
        config.setEternal(true);
        Searchable searchable = new Searchable().searchAttribute(new SearchAttribute().name("attr").expression("value.getAttr()"));
        config.addSearchable(searchable);

        cacheManager.addCache(new Cache(config));

        class Value {
            private final Object attr;

            Value(Object attr) {
                this.attr = attr;
            }

            Object getAttr() {
                return attr;
            }
        }

        Ehcache cache = cacheManager.getEhcache("test");
        cache.put(new Element(1, new Value("foo")));

        Query query = cache.createQuery();
        query.includeKeys();
        query.addCriteria(cache.getSearchAttribute("attr").le(4));
        query.end();

        try {
            query.execute();
            fail();
        } catch (SearchException se) {
            // expected since the criteria wants INT, but actual attribute value is STRING
        }

        // with proper type search will execute
        cache.put(new Element(1, new Value(4)));
        assertEquals(1, query.execute().all().iterator().next().getKey());
    }

    @Test
    public void testEmptyQueries() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        {
            Query query = cache.createQuery();
            query.end();
            try {
                query.execute();
                fail();
            } catch (SearchException e) {
                System.err.println("Expected " + e);
            }
        }

        {
            Attribute<Integer> age = cache.getSearchAttribute("age");
            Query query = cache.createQuery();
            query.addCriteria(age.ne(35));
            query.end();
            try {
                query.execute();
                fail();
            } catch (SearchException e) {
                System.err.println("Expected " + e);
            }
        }
    }

    @Test
    public void testIncludeValues() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        {
            Query query = cache.createQuery();
            query.includeValues();
            query.end();
            Results results = query.execute();
            assertTrue(results.hasValues());
            assertEquals(4, results.size());
            int ageSum = 0;
            for (Result result : results.all()) {
                System.err.println(result.toString()); // exercise toString()

                Person p = (Person) result.getValue();
                ageSum += p.getAge();
                try {
                    result.getKey();
                    fail();
                } catch (SearchException se) {
                    // expected since keys not included
                }
            }

            assertEquals(123, ageSum);
        }

        {
            Query query = cache.createQuery();
            query.includeKeys();
            query.end();
            Results results = query.execute();
            assertFalse(results.hasValues());
            assertEquals(4, results.size());
            for (Result result : results.all()) {
                try {
                    result.getValue();
                    fail();
                } catch (SearchException se) {
                    // expected since keys not included
                }
            }
        }
    }

    @Test
    public void testSearchWithPinnedKeys() {
        Ehcache cache1 = cacheManager.getEhcache("cache1");
        Ehcache cache2 = cacheManager.getEhcache("cache2");
        Ehcache bean_attributes = cacheManager.getEhcache("bean-attributes");
        cache1.setPinned(0, true);
        cache2.setPinned(0, true);
        bean_attributes.setPinned(0, true);

        // uses expression attribute extractors
        basicQueries(cache1);

        // uses a "custom" attribute extractor too
        basicQueries(cache2);

        // uses bean attributes
        basicQueries(bean_attributes);

        cache1.setPinned(1, true);
        cache2.setPinned(1, true);
        bean_attributes.setPinned(1, true);

        // uses expression attribute extractors
        basicQueries(cache1);

        // uses a "custom" attribute extractor too
        basicQueries(cache2);

        // uses bean attributes
        basicQueries(bean_attributes);

    }

    @Test
    public void testBasicGroupBy() {
        Ehcache cache = cacheManager.getEhcache("cache1");
        assertTrue(cache.isSearchable());

        int numOfDepts = 10;
        int numOfMalesPerDept = 100;
        int numOfFemalesPerDept = 100;

        for (int i = 0; i < numOfDepts; i++) {
            for (int j = 0; j < numOfMalesPerDept; j++) {
                cache.put(new Element("male" + i + "-" + j, new Person("male" + j, j, Gender.MALE, "dept" + i)));
            }

            for (int j = 0; j < numOfFemalesPerDept; j++) {
                cache.put(new Element("female" + i + "-" + j, new Person("female" + j, j, Gender.FEMALE, "dept" + i)));
            }
        }

        Query query;
        Results results;

        query = cache.createQuery();
        query.includeAttribute(cache.getSearchAttribute("dept"));
        query.includeAttribute(cache.getSearchAttribute("gender"));
        query.includeAggregator(cache.getSearchAttribute("age").sum());
        query.includeAggregator(cache.getSearchAttribute("age").min());
        query.includeAggregator(cache.getSearchAttribute("age").max());
        query.addGroupBy(cache.getSearchAttribute("dept"));
        query.addOrderBy(cache.getSearchAttribute("dept"), Direction.DESCENDING);
        query.addOrderBy(cache.getSearchAttribute("gender"), Direction.ASCENDING);
        query.addGroupBy(cache.getSearchAttribute("gender"));
        query.end();

        results = query.execute();

        assertEquals(numOfDepts * 2, results.size());

        int i = 1;
        for (Iterator<Result> iter = results.all().iterator(); iter.hasNext();) {
            Result maleResult = iter.next();
            System.out.println("XXXXXXXXX: " + maleResult);
            assertEquals("dept" + (numOfDepts - i), maleResult.getAttribute(cache.getSearchAttribute("dept")));
            assertEquals(Gender.MALE, maleResult.getAttribute(cache.getSearchAttribute("gender")));

            assertTrue(maleResult instanceof GroupedResultImpl);
            Map<String, Object> groupByValues = ((GroupedResultImpl) maleResult).getGroupByValues();
            assertEquals(2, groupByValues.size());
            assertEquals("dept" + (numOfDepts - i), groupByValues.get("dept"));
            assertEquals(Gender.MALE, groupByValues.get("gender"));

            List aggregateResults = maleResult.getAggregatorResults();
            assertEquals(3, aggregateResults.size());
            assertEquals(numOfMalesPerDept * (numOfMalesPerDept - 1) / 2, ((Long) aggregateResults.get(0)).intValue());
            assertEquals(0, ((Integer) aggregateResults.get(1)).intValue());
            assertEquals(numOfMalesPerDept - 1, ((Integer) aggregateResults.get(2)).intValue());

            Result femaleResult = iter.next();
            System.out.println("XXXXXXXXX: " + femaleResult);

            assertEquals("dept" + (numOfDepts - i), femaleResult.getAttribute(cache.getSearchAttribute("dept")));
            assertEquals(Gender.FEMALE, femaleResult.getAttribute(cache.getSearchAttribute("gender")));

            assertTrue(femaleResult instanceof GroupedResultImpl);
            groupByValues = ((GroupedResultImpl) femaleResult).getGroupByValues();
            assertEquals(2, groupByValues.size());
            assertEquals("dept" + (numOfDepts - i), groupByValues.get("dept"));
            assertEquals(Gender.FEMALE, groupByValues.get("gender"));

            aggregateResults = femaleResult.getAggregatorResults();
            assertEquals(3, aggregateResults.size());
            assertEquals(numOfFemalesPerDept * (numOfFemalesPerDept - 1) / 2, ((Long) aggregateResults.get(0)).intValue());
            assertEquals(0, ((Integer) aggregateResults.get(1)).intValue());
            assertEquals(numOfFemalesPerDept - 1, ((Integer) aggregateResults.get(2)).intValue());

            i++;
        }

    }

    private void verify(Ehcache cache, Query query, Integer... expectedKeys) {
        Results results = query.execute();
        assertEquals(expectedKeys.length, results.size());
        if (expectedKeys.length != 0) {
            assertTrue(results.hasKeys());
        }
        assertFalse(results.hasAttributes());

        Set<Integer> keys = new HashSet<Integer>(Arrays.asList(expectedKeys));

        System.err.println(results.toString()); // call toString() just to make sure it doesn't blow up

        for (Result result : results.all()) {
            System.err.println(result.toString()); // call toString() just to make sure it doesn't blow up

            int key = (Integer) result.getKey();
            if (!keys.remove(key)) {
                throw new AssertionError("unexpected key: " + key);
            }
        }
    }

    private void verifyOrdered(Ehcache cache, Query query, Integer... expectedKeys) {
        Results results = query.execute();
        assertEquals(results.size(), expectedKeys.length);

        int pos = 0;
        for (Result result : results.all()) {
            Object expectedKey = expectedKeys[pos++];
            assertEquals(expectedKey, result.getKey());
        }
    }

}
