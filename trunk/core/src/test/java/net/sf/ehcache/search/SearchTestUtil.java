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
