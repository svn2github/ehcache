/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.store.chm;

import java.util.Random;
import net.sf.ehcache.Element;

/**
 *
 * @author cdennis
 */
public class SelectableConcurrentHashMap extends ConcurrentHashMap<Object, Element> {

    private final Random rndm = new Random();

    public SelectableConcurrentHashMap(int initialCapacity, float loadFactor, int concurrency) {
        super(initialCapacity, loadFactor, concurrency);
    }

    public Element[] getRandomValues(int size) {
        Element[] sampled = new Element[size];

        int index = 0;
        while (!isEmpty()) {
            Segment<Object, Element> seg = segmentFor(rndm.nextInt());
            for (int i = 0; i < seg.count; i++) {
                for (HashEntry<Object, Element> e = seg.getFirst(rndm.nextInt()); e != null; e = e.next) {
                    Element value = e.value;
                    if (value != null) {
                        sampled[index++] = value;
                        if (index == sampled.length) {
                            return sampled;
                        }
                    }
                }
            }
        }
        return sampled;
    }
}
