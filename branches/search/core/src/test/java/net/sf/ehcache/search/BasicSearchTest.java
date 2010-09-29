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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.search.expression.Or;

public class BasicSearchTest extends TestCase {

    public void testBasic() {
        CacheManager cacheManager = new CacheManager(getClass().getResource("/ehcache-search.xml"));
        Cache cache = cacheManager.getCache("cache1");

        cache.put(new Element(1, new Person("Tim Eck", 35, Gender.MALE)));
        cache.put(new Element(2, new Person("Loretta Johnson", 23, Gender.FEMALE)));
        cache.put(new Element(3, new Person("Ari Zilka", 35, Gender.MALE)));
        cache.put(new Element(4, new Person("Nabib El-Rahman", 30, Gender.MALE)));

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

        Cache cache2 = cacheManager.getCache("cache2");
        System.err.println(cache2);
    }

    private void verify(Cache cache, Query query, Integer... expectedKeys) {
        Results results = query.execute();
        assertEquals(results.size(), expectedKeys.length);

        Set<Integer> keys = new HashSet<Integer>(Arrays.asList(expectedKeys));

        for (Result result : results.all()) {
            int key = (Integer) result.getKey();
            if (!keys.remove(key)) {
                throw new AssertionError("unexpected key: " + key);
            }
            assertEquals(cache.get(key).getObjectValue(), result.getValue());
        }
    }

    enum Gender {
        MALE, FEMALE;
    }

    private static class Person {

        private final String name;
        private final int age;
        private final Gender gender;

        public Person(String name, int age, Gender gender) {
            this.name = name;
            this.age = age;
            this.gender = gender;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public Gender getGender() {
            return gender;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(name:" + name + ", age:" + age + ", sex:" + gender.name().toLowerCase() + ")";
        }
    }

}
