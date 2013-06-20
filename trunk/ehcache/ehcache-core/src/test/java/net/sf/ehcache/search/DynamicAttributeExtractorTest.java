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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.*;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Person.Gender;
import net.sf.ehcache.search.aggregator.Aggregators;
import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DynamicAttributeExtractorTest {
    
    private CacheManager cm;
    private Ehcache cache;
    
    private Attribute<Integer> age;
    private Attribute<String> name;
    private Attribute<String> dept;
    
    private final Attribute<Gender> gender = new Attribute<Gender>("gender"); // dynamically extracted
    
    private final DynamicAttributesExtractor indexer = new DynamicAttributesExtractor() {

        @Override
        public Map<String, Gender> attributesFor(Element element) {
           Person p = (Person) element.getObjectValue();
           return Collections.singletonMap("gender", p.getGender());
        }
      };

    @Before
    public void setUp() throws Exception {
        Configuration cfg = new Configuration();
        CacheConfiguration cacheCfg = new CacheConfiguration("searchCache", 1000);
        Searchable s = new Searchable();
        s.keys(true);
        s.allowDynamicIndexing(true);
        s.addSearchAttribute(new SearchAttribute().name("age"));
        s.addSearchAttribute(new SearchAttribute().name("name"));
        s.addSearchAttribute(new SearchAttribute().name("department"));
        cacheCfg.searchable(s);
        cfg.addCache(cacheCfg);
        
        cm = new CacheManager(cfg);
        cache = cm.getCache("searchCache");
        cache.registerDynamicAttributesExtractor(indexer);
        SearchTestUtil.populateData(cache);
        
        age = cache.getSearchAttribute("age");
        dept = cache.getSearchAttribute("department");
        name = cache.getSearchAttribute("name");
    }

    @After
    public void tearDown() throws Exception {
        cm.shutdown();
    }

    private void verifyOrdered(Query q, int... expectedKeys) {
        Results results = q.execute();
        assertEquals(expectedKeys.length, results.size());
        if (expectedKeys.length == 0) {
            assertFalse(results.hasKeys());
        } else {
            assertTrue(results.hasKeys());
        }
        assertFalse(results.hasAttributes());

        int i = 0;
        for (Result result : results.all()) {
            
            int key = (Integer) result.getKey();
            assertEquals(expectedKeys[i++], key);
        }
    }

    private void verify(Query q, Integer... expectedKeys) {
        Results results = q.execute();
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
    public void testBasicCriteria() {
        Query q = cache.createQuery();
        q.addCriteria(gender.eq(Gender.MALE));
        q.includeKeys();
        verify(q, 1, 3, 4);
        
        q.addCriteria(age.lt(31));
        verify(q, 4);
        
        q = cache.createQuery();
        q.includeKeys();
        q.addCriteria(gender.ne(Gender.MALE).and(dept.ilike("eng?")));
        verify(q, 2);
        
    }

    @Test
    public void testSorting() {
        Query q = cache.createQuery().includeKeys();
        q.addOrderBy(gender, Direction.DESCENDING);
        q.addOrderBy(name, Direction.ASCENDING);
        verifyOrdered(q, 2, 3, 4, 1);
    }
    
    @Test
    public void testGroupBy() {
        Query q = cache.createQuery().addGroupBy(gender);
        q.includeAggregator(age.max());
        Results res = q.execute();
        assertEquals(2, res.size());
    }
    
    @Test
    public void testAggregators() {
        Query q = cache.createQuery().addCriteria(gender.eq(Gender.MALE));
        q.includeAggregator(Aggregators.count());
        q.addCriteria(dept.ilike("sales*").not());
        Results res = q.execute();
        assertEquals(1, res.size());
        assertEquals(3, res.all().get(0).getAggregatorResults().get(0));
    }
}
