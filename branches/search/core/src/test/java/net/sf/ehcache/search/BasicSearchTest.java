/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.search.Person.Gender;
import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.AggregatorException;
import net.sf.ehcache.search.aggregator.Average;
import net.sf.ehcache.search.aggregator.Count;
import net.sf.ehcache.search.aggregator.Max;
import net.sf.ehcache.search.aggregator.Min;
import net.sf.ehcache.search.aggregator.Sum;
import net.sf.ehcache.search.expression.Or;

public class BasicSearchTest extends TestCase {

    public void testQueryBuilder() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        Ehcache cache = cacheManager.getEhcache("cache1");

        Query query1 = cache.createQuery();
        Query query2 = cache.createQuery();

        // query instances should be unique
        assertFalse(query1 == query2);

        // null checks
        try {
            query1.add(null);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.addOrder(null, Direction.ASCENDING);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.addOrder(new Attribute("foo"), null);
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.includeAggregator(null, new Attribute("foo"));
            fail();
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            query1.includeAggregator(new Count(), null);
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
            query1.includeAttribute(new Attribute[] { new Attribute("foo"), null });
            fail();
        } catch (NullPointerException npe) {
            // expected
        }

        // freeze query
        query1.end();

        try {
            query1.add(new Attribute("foo").le(35));
            fail();
        } catch (SearchException se) {
            // expected
        }
        try {
            query1.addOrder(new Attribute("foo"), Direction.ASCENDING);
            fail();
        } catch (SearchException se) {
            // expected
        }
        try {
            query1.includeAggregator(new Count(), new Attribute("foo"));
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
            query1.includeValues();
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

    public void testRange() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
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

    public void testBasic() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));

        // uses expression attribute extractors
        basicQueries(cacheManager.getEhcache("cache1"));

        // uses a "custom" attribute extractor too
        basicQueries(cacheManager.getEhcache("cache2"));
    }

    public void testCustomAggregator() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");

        Query query = cache.createQuery();
        query.includeAggregator(new Aggregator<Integer>() {
            private int doubledSum;

            public void accept(Object input) throws AggregatorException {
                if (doubledSum == 0) {
                    doubledSum = (2 * (Integer) input);
                } else {
                    doubledSum += (2 * (Integer) input);
                }
            }

            public Integer aggregateResult() {
                return doubledSum;
            }

        }, age);
        query.end();

        Results results = query.execute();

        int count = (Integer) results.aggregateResult();
        assertEquals(246, count);
    }

    public void testBuiltinFunctions() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");

        {
            Query query = cache.createQuery();
            query.includeAggregator(new Count(), age);
            query.end();

            Results results = query.execute();

            int count = (Integer) results.aggregateResult();
            assertEquals(4, count);
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(new Max(), age);
            query.end();

            Results results = query.execute();

            int max = (Integer) results.aggregateResult();
            assertEquals(35, max);
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(new Min(), age);
            query.end();

            Results results = query.execute();

            int min = (Integer) results.aggregateResult();
            assertEquals(23, min);
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(new Sum(), age);
            query.end();

            Results results = query.execute();

            long sum = (Long) results.aggregateResult();
            assertEquals(123, sum);
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(new Average(), age);
            query.end();

            Results results = query.execute();

            double avg = (Double) results.aggregateResult();
            assertEquals(30.75D, avg);
        }

        {
            Query query = cache.createQuery();
            query.includeAggregator(new Min(), age);
            query.includeAggregator(new Max(), age);
            query.end();

            Results results = query.execute();

            List aggregateResults = (List) results.aggregateResult();
            assertEquals(23, aggregateResults.get(0));
            assertEquals(35, aggregateResults.get(1));
        }

    }

    public void testMaxResults() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<Person.Gender> gender = cache.getSearchAttribute("gender");

        Query query = cache.createQuery();
        query.includeKeys();
        query.add(age.ne(35));
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
        query.add(age.ne(35));
        query.maxResults(0);
        query.end();

        results = query.execute();
        assertEquals(0, results.size());

        query = cache.createQuery();
        query.includeKeys();
        query.add(age.ne(35));
        query.maxResults(2);
        query.end();

        results = query.execute();
        assertEquals(2, results.size());

        query = cache.createQuery();
        query.includeKeys();
        query.add(age.ne(35));
        query.maxResults(2);
        query.end();

        results = query.execute();
        assertEquals(2, results.size());

        query = cache.createQuery();
        query.includeKeys();
        query.add(age.ne(35));
        query.maxResults(-1);
        query.end();

        results = query.execute();
        assertEquals(2, results.size());
    }

    public void testAttributeQuery() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<Person.Gender> gender = cache.getSearchAttribute("gender");

        Query query = cache.createQuery();
        // not including keys
        query.add(age.ne(35));
        query.includeAttribute(age, gender);
        query.end();

        Results results = query.execute();
        assertFalse(results.hasKeys());
        assertFalse(results.isAggregate());

        for (Result result : results.all()) {
            try {
                result.getKey();
                fail();
            } catch (SearchException se) {
                // expected
            }

            try {
                result.getValue();
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
        query.add(age.ne(35));
        query.end();
        verify(cache, query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.add(cache.getSearchAttribute("age").lt(30));
        query.end();
        query.execute();
        verify(cache, query, 2);

        query = cache.createQuery();
        query.includeKeys();
        query.add(cache.getSearchAttribute("age").le(30));
        query.end();
        query.execute();
        verify(cache, query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.add(cache.getSearchAttribute("age").in(new HashSet(Arrays.asList(23, 35))));
        query.end();
        query.execute();
        verify(cache, query, 1, 2, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.add(cache.getSearchAttribute("age").gt(30));
        query.end();
        query.execute();
        verify(cache, query, 1, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.add(cache.getSearchAttribute("age").between(23, 35, true, false));
        query.end();
        query.execute();
        verify(cache, query, 2, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.add(cache.getSearchAttribute("age").ge(30));
        query.end();
        query.execute();
        verify(cache, query, 1, 3, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.add(new Or(cache.getSearchAttribute("age").eq(35), cache.getSearchAttribute("gender").eq(Gender.FEMALE)));
        query.end();
        verify(cache, query, 1, 2, 3);
    }

    public void testOrdering() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<String> name = cache.getSearchAttribute("name");

        Query query;

        query = cache.createQuery();
        query.includeKeys();
        // no critera -- select all elements
        query.addOrder(age, Direction.DESCENDING);
        query.addOrder(name, Direction.ASCENDING);
        query.end();

        verifyOrdered(cache, query, 3, 1, 4, 2);

        query = cache.createQuery();
        query.includeKeys();
        // no critera -- select all elements
        query.addOrder(age, Direction.DESCENDING);
        query.addOrder(name, Direction.ASCENDING);
        query.maxResults(2);
        query.end();

        verifyOrdered(cache, query, 3, 1);
    }

    public void testLike() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        Ehcache cache = cacheManager.getEhcache("cache1");
        SearchTestUtil.populateData(cache);

        Attribute<String> name = cache.getSearchAttribute("name");

        Query query;

        query = cache.createQuery();
        query.includeKeys();
        query.add(new Or(name.like("tim*"), name.like("ari*")));
        query.end();

        verify(cache, query, 3, 1);

        cache.removeAll();
        cache.put(new Element(1, new Person("Test \\ Bob * ?", 35, Gender.MALE)));
        cache.put(new Element(2, new Person("(..Test", 35, Gender.MALE)));
        cache.put(new Element(3, new Person("lowercase", 35, Gender.MALE)));
        cache.put(new Element(4, new Person("UPPERCASE", 35, Gender.MALE)));
        cache.put(new Element(5, new Person("MiXeD", 35, Gender.MALE)));

        query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("Test \\\\ Bob \\* \\?"));
        query.end();

        verify(cache, query, 1);

        query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("Test*"));
        query.end();

        verify(cache, query, 1);

        query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("Test*\\?"));
        query.end();

        verify(cache, query, 1);

        query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("(..*"));
        query.end();

        verify(cache, query, 2);

        query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("Lowercase"));
        query.end();

        verify(cache, query, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("LOWER*"));
        query.end();

        verify(cache, query, 3);

        query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("uppercase"));
        query.end();

        verify(cache, query, 4);

        query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("mixed"));
        query.end();

        verify(cache, query, 5);
    }

    public void testTypeChecking() {
        CacheManager cm = new CacheManager(new Configuration().defaultCache(new CacheConfiguration()));

        CacheConfiguration config = new CacheConfiguration("test", 0);
        config.setOverflowToDisk(false);
        config.diskPersistent(false);
        config.setEternal(true);
        config.addSearchAttribute(new SearchAttribute().name("attr").expression("value.getAttr()"));
        cm.addCache(new Cache(config));

        class Value {
            private final Object attr;

            Value(Object attr) {
                this.attr = attr;
            }

            Object getAttr() {
                return attr;
            }
        }

        Ehcache cache = cm.getEhcache("test");
        cache.put(new Element(1, new Value("foo")));

        Query query = cache.createQuery();
        query.includeKeys();
        query.add(cache.getSearchAttribute("attr").le(4));
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

    private void verify(Ehcache cache, Query query, Integer... expectedKeys) {
        Results results = query.execute();
        assertEquals(results.size(), expectedKeys.length);
        assertTrue(results.hasKeys());

        Set<Integer> keys = new HashSet<Integer>(Arrays.asList(expectedKeys));

        for (Result result : results.all()) {
            int key = (Integer) result.getKey();
            if (!keys.remove(key)) {
                throw new AssertionError("unexpected key: " + key);
            }
            assertEquals(cache.get(key).getObjectValue(), result.getValue());
        }
    }

    private void verifyOrdered(Ehcache cache, Query query, Integer... expectedKeys) {
        Results results = query.execute();
        assertEquals(results.size(), expectedKeys.length);

        int pos = 0;
        for (Result result : results.all()) {
            Object expectedKey = expectedKeys[pos++];
            assertEquals(expectedKey, result.getKey());
            assertEquals(cache.get(expectedKey).getObjectValue(), result.getValue());
        }
    }

}
