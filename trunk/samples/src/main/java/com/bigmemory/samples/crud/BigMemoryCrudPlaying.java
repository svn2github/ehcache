package com.bigmemory.samples.crud;
/*
 * Released to the public domain, as explained at  http://creativecommons.org/licenses/publicdomain
 */

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;

import com.bigmemory.samples.model.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class BigMemoryCrudPlaying {
  private CacheManager manager;
  private Cache bigMemory;

  public BigMemoryCrudPlaying() {
    /** Setting up the bigMemory configuration **/
    Configuration configuration = new Configuration();
     manager = new CacheManager(configuration.maxBytesLocalHeap(32, MemoryUnit.MEGABYTES)
        .name("new-manager").maxBytesLocalOffHeap(32, MemoryUnit.MEGABYTES));

    CacheConfiguration cacheConfiguration = new CacheConfiguration("bigMemory-crud", 0);
    manager.addCache(new Cache(cacheConfiguration));
    bigMemory = manager.getCache("bigMemory-crud");
  }


  public void runTests() throws IOException {

    //put value
    System.out.println("**** Put key 1 / value timEck into BigMemory ****");
    final Person timEck = new Person("Tim Eck", 35, Person.Gender.MALE,
        "eck street", "San Mateo", "CA");
    bigMemory.put(new Element("1", timEck));
    read();

    //get value
    System.out.println("**** Retrieve key 1 from BigMemory. ****");
    final Element element = bigMemory.get("1");
    System.out.println("The value for key 1 is  " + element.getValue());
    read();

    //update value
    System.out.println("**** Update value for key 1 to pamelaJones ****");
    final Person pamelaJones = new Person("Pamela Jones", 23, Person.Gender.FEMALE,
        "berry st", "Parsippany", "LA");
    bigMemory.put(new Element("1", pamelaJones));
    final Element updated = bigMemory.get("1");
    System.out.println("The value for key 1 is now " + updated.getValue() + ". key 1 has been updated.");
    read();

    //delete value
    System.out.println("**** Delete key 1 from bigMemory. ****");
    bigMemory.remove("1");
    System.out.println("Retrieve key 1 from bigMemory.");
    final Element removed = bigMemory.get("1");
    System.out.println("Value for key 1 is " + removed + ". Key 1 has been deleted.");
    read();

    System.out.println("Number of element in bigMemory : " + bigMemory.getSize());
    read();

    //put all
    System.out.println("**** Put 5 key/value into BigMemory ****");
    bigMemory.putAll(get5Elements());
    System.out.println("Number of element in bigMemory : " + bigMemory.getSize());
    read();

    //get all
    System.out.println("**** Get elements of keys 1,2 and 3  ****");
    final Map<Object, Element> elementsMap = bigMemory.getAll(getFirst3Keys());
    for (Element currentElement : elementsMap.values()) {
      System.out.println(currentElement);
    }
    read();

    //remove all
    System.out.println("**** Remove the element with keys 1,2,3 into BigMemory ****");
    bigMemory.removeAll(getFirst3Keys());
    System.out.println("Number of element in bigMemory : " + bigMemory.getSize());

    //removing all elements
    bigMemory.removeAll();
    read();
    System.out.println("Number of element in bigMemory : " + bigMemory.getSize());

    manager.shutdown();


  }

  private Collection<String> getFirst3Keys() {
    Collection<String> keys = new ArrayList<String>();
    keys.add("1");
    keys.add("2");
    keys.add("3");
    return keys;
  }

  private Collection<Element> get5Elements() {
    Collection<Element> elements = new ArrayList<Element>();
    elements.add(new Element("1", new Person("Tim Eck", 35, Person.Gender.MALE,
        "eck street", "San Mateo", "CA")));
    elements.add(new Element("2", new Person("Pamela Jones", 23, Person.Gender.FEMALE,
        "berry st", "Parsippany", "LA")));
    elements.add(new Element("3", new Person("Ari Zilka", 25, Person.Gender.MALE,
        "big wig", "Beverly Hills", "NJ")));
    elements.add(new Element("4", new Person("Ari gold", 45, Person.Gender.MALE,
        "cool agent", "Madison", "WI")));
    elements.add(new Element("5", new Person("Nabib El-Rahman", 30, Person.Gender.MALE,
        "dah man", "Bangladesh", "MN")));

    return elements;
  }

  private void read() throws IOException {
    System.err.println("\nhit enter to continue");
    System.in.read();
  }

  public static void main(String[] args) throws IOException {
    new BigMemoryCrudPlaying().runTests();
  }
}
