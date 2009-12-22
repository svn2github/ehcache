/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
package net.sf.ehcache.store.chm;

import java.util.Random;
import net.sf.ehcache.Element;

/**
 * SelectableConcurrentHashMap subclasses a repackaged version of ConcurrentHashMap
 * ito allow efficient random sampling of the map values.
 * <p>
 * The random sampling technique involves randomly selecting a map Segment, and then
 * selecting a number of random entry chains from that segment.
 * 
 * @author Chris Dennis
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
