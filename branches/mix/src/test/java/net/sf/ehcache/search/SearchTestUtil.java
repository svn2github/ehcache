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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.search.Person.Gender;

/**
 * Test utilities used by multiple classes
 *
 * @author Greg Luck
 */
public class SearchTestUtil {

    /**
     * Used to populate the search with data
     *
     * @param cache
     */
    public static void populateData(Ehcache cache) {
        cache.removeAll();
        cache.put(new Element(1, new Person("Tim Eck", 35, Gender.MALE)));
        cache.put(new Element(2, new Person("Loretta Johnson", 23, Gender.FEMALE)));
        cache.put(new Element(3, new Person("Ari Zilka", 35, Gender.MALE)));
        cache.put(new Element(4, new Person("Nabib El-Rahman", 30, Gender.MALE)));
        // cache.put(new Element(5, new Person("Greg Luck", 43, Gender.MALE)));
        // cache.put(new Element(6, new Person("Kellie Luck", 41, Gender.MALE)));
        // cache.put(new Element(7, new Person("Curtis Luck", 9, Gender.MALE)));
        // cache.put(new Element(8, new Person("Lewis Luck", 9, Gender.MALE)));
    }

}
