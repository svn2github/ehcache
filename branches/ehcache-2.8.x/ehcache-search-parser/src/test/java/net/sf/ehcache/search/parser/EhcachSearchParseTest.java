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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.expression.EqualTo;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EhcachSearchParseTest {

    private static void populate(Ehcache cache) throws java.text.ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 10; i < 30; i++) {
            HashMap<String, Object> nv = new HashMap<String, Object>();
            nv.put("zip", "210" + i);
            nv.put("age", i);
            nv.put("date", formatter.parse("20" + i + "-06-01"));
            CacheValue cv = new CacheValue("John Frisk " + i, nv);
            cache.put(new Element(i, cv));
        }
        Assert.assertEquals(cache.getSize(), 20);

    }

    private static Ehcache makeCache() {


        Configuration cmConfig = new Configuration().name("searchTestCM");

        CacheManager cm = new CacheManager(cmConfig);


        Searchable searchable = new Searchable();
        SearchAttribute age = new SearchAttribute().name("age").className(Indexer.class.getName());
        SearchAttribute zip = new SearchAttribute().name("zip").className(Indexer.class.getName());
        SearchAttribute date = new SearchAttribute().name("date").className(Indexer.class.getName());


        searchable.addSearchAttribute(age);
        searchable.addSearchAttribute(zip);
        searchable.addSearchAttribute(date);

        CacheConfiguration conf = new CacheConfiguration()
            .name("cache1")
            .eternal(true)
            .maxEntriesLocalHeap(1000)
            .searchable(searchable);

        Cache c1 = new Cache(conf);
        cm.addCache(c1);
        Ehcache cache = cm.getEhcache("cache1");


        return cache;
    }

    private Ehcache cache;
    private CacheManager cacheManager;
    private List<Ehcache> ehcaches = new ArrayList<Ehcache>();

    @Before
    public void before() throws java.text.ParseException {
        cache = makeCache();
        ehcaches.add(cache);
        cacheManager = cache.getCacheManager();
        populate(cache);
    }

    @After
    public void after() {
        cache.getCacheManager().shutdown();
    }

    @Test
    public void testSanityEhcacheSearch() {
        Results res = cache.createQuery().addCriteria(new EqualTo("age", 12)).includeKeys().includeValues().end().execute();
        Assert.assertEquals(res.size(), 1);
        Assert.assertTrue(res.hasKeys());
        Assert.assertTrue(res.hasValues());
        Assert.assertFalse(res.hasAggregators());
    }

    @Test
    public void testSimpleParserSearch() throws ParseException {
        String st = "select key, value from cache1 where age = 12";
        QueryManagerImpl queryParser = new QueryManagerImpl(ehcaches);
        Results res = queryParser.search(getCache(st), st);
        Assert.assertEquals(res.size(), 1);
        Assert.assertTrue(res.hasKeys());
        Assert.assertTrue(res.hasValues());
        Assert.assertFalse(res.hasAggregators());
        Assert.assertFalse(res.hasAttributes());
        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(cv.getNvPairs().get("age"), 12);
        Assert.assertEquals(cv.getValue(), "John Frisk " + k);

    }


    @Test
    public void testParserAndSearch() throws ParseException {
        String st = "select key, value from cache1 where (age > 11 and age < 13)";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(res.size(), 1);

        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(cv.getNvPairs().get("age"), 12);
        Assert.assertEquals(cv.getValue(), "John Frisk " + k);

    }


    @Test
    public void testParserIsBetweenSearch() throws ParseException {
        String st = "select key, value from cache1 where (age isbetween 11 13)";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(res.size(), 1);

        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(cv.getNvPairs().get("age"), 12);
        Assert.assertEquals(cv.getValue(), "John Frisk " + k);

    }


    @Test
    public void testParserIsBetweenInclusiveSearchPlusOrder() throws ParseException {
        String st = "select key,value from cache1 where (age isbetween [ 11 13 ]) order by age";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(res.size(), 3);

        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(cv.getNvPairs().get("age"), 11);
        Assert.assertEquals(cv.getValue(), "John Frisk " + k);

    }


    @Test
    public void testParserBetweenSearchPlusOrder() throws ParseException {
        String st = "select key,value from cache1 where (age between 11 and 13) order by age";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(res.size(), 3);

        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(cv.getNvPairs().get("age"), 11);
        Assert.assertEquals(cv.getValue(), "John Frisk " + k);

    }

    @Test
    public void testParserNestedAnd() throws ParseException {
        String st = "select key, value from cache1 where (age > 11 and age < 13 and zip='21012') order by age";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(res.size(), 1);

        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(cv.getNvPairs().get("age"), 12);
        Assert.assertEquals(cv.getNvPairs().get("zip"), "21012");
        Assert.assertEquals(cv.getValue(), "John Frisk " + k);

    }

    @Test
    public void testParserIlike() throws ParseException {
        String st = "select key, value from cache1 where (zip ilike '2101?') order by zip";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(10, res.size());

        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(10, cv.getNvPairs().get("age"));
        Assert.assertEquals("21010", cv.getNvPairs().get("zip"));
        Assert.assertEquals("John Frisk " + k, cv.getValue());

    }


    @Test
    public void testParserIntCast() throws ParseException {
        String st = "select key, value from cache1 where age = (int)12";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(res.size(), 1);

        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(cv.getNvPairs().get("age"), 12);
        Assert.assertEquals(cv.getValue(), "John Frisk " + k);

    }

    @Test
    public void testParserDate() throws ParseException {
        String st = "select key, value from cache1 where ( date > (date)'2011-06-01' and  date < (date)'2013-06-01')";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(1, res.size());

        Result r = res.all().iterator().next();
        Assert.assertTrue(r.getKey() != null);
        Assert.assertTrue(r.getValue() != null);

        CacheValue cv = (CacheValue)r.getValue();
        Integer k = (Integer)r.getKey();

        Assert.assertEquals(cv.getNvPairs().get("age"), 12);
        Assert.assertEquals(cv.getValue(), "John Frisk " + k);
    }


    @Test
    public void testParserAttributeRetrieval() throws ParseException {
        String st = "select age from cache1 where  date > (date)'2011-06-01' order by age ";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(18, res.size());
        Assert.assertFalse(res.hasKeys());
        Assert.assertFalse(res.hasValues());
        Assert.assertTrue(res.hasAttributes());
        int shouldBe = 12;
        for (Result r : res.all()) {
            Assert.assertEquals((Integer)shouldBe++, r.getAttribute(new Attribute<Integer>("age")));
        }
    }

    @Test
    public void testInClause() throws ParseException {
        String st = "select * from cache1 where age in (10, 11, 12, 13, 14) order by age asc ";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(5, res.size());
        Assert.assertFalse(res.hasKeys());
        Assert.assertFalse(res.hasValues());
        Assert.assertTrue(res.hasAttributes());
        int shouldBe = 10;
        int age;
        for (Result r : res.all()) {
            age = r.getAttribute(new Attribute<Integer>("age"));
            Assert.assertEquals(shouldBe++, age);
        }
    }


    @Test
    public void testParserAggregatorsRetrieval() throws ParseException {
        String st = "select sum(age), count(zip) from cache1 where  date > (date)'2011-06-01' ";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(1, res.size());
        Assert.assertFalse(res.hasKeys());
        Assert.assertFalse(res.hasValues());
        Assert.assertFalse(res.hasAttributes());
        Assert.assertTrue(res.hasAggregators());
        long total = 0;
        for (int i = 12; i < 30; i++) {
            total = total + i;
        }
        Long ageSum = (Long)res.all().iterator().next().getAggregatorResults().get(0);
        Integer zipCount = (Integer)res.all().iterator().next().getAggregatorResults().get(1);
        Assert.assertEquals((Long)total, ageSum);
        Assert.assertEquals((Integer)18, zipCount);

    }

    @Test
    public void testParserSelectStar() throws ParseException {
        String st = "select * from cache1 where  date >= (date)'2020-06-01' order by age ";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(10, res.size());
        Assert.assertFalse(res.hasKeys());
        Assert.assertFalse(res.hasValues());
        Assert.assertTrue(res.hasAttributes());
    }


    @Test
    public void testParserSelectStarKeyValue() throws ParseException {
        String st = "select *,key,value from cache1 where  date >= (date)'2020-06-01' order by age ";
        Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
        Assert.assertEquals(10, res.size());
        Assert.assertTrue(res.hasKeys());
        Assert.assertTrue(res.hasValues());
        Assert.assertTrue(res.hasAttributes());
    }

    @Test
    public void testDateFormats() throws ParseException {
        String[] dateFormats = {
            "2009-06-01T00:00:00.555",
            "2009-06-01T01:01:00.555+01",
            "2009-06-01T01:01:00.555+0104",
            "2009-06-01T01:01:00.555+01:04",
            "2009-06-01",
            "06/01/2009",
            "06/01/2009T01:01:00.555+01",
            "06/01/2009T01:01:00.555+0104",
            "06/01/2009T01:01:00.555+01:04",
            "6/1/2009"
        };

        for (String dateFormat : dateFormats) {
            String st = "select * from cache1 where  date >= (date)'" + dateFormat + "'";
            Results res = new QueryManagerImpl(ehcaches).search(getCache(st), st);
            Assert.assertEquals(st, 20, res.size());
        }
    }

    private Cache getCache(String st) {
        QueryManagerImpl queryParser = new QueryManagerImpl(ehcaches);
        Map<String, String> m = queryParser.extractSearchCacheName(st);
        String cacheName = m.keySet().iterator().next();
        return cacheManager.getCache(cacheName);
    }
}
