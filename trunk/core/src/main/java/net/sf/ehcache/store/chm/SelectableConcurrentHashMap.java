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
package net.sf.ehcache.store.chm;

import java.util.ArrayList;
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

    public Element[] getRandomValues(final int size, Object keyHint) {
        ArrayList<Element> sampled = new ArrayList<Element>(size * 2);

        // pick a random starting point in the map
        int randomHash = rndm.nextInt();

        final int segmentStart;
        if (keyHint == null) {
            segmentStart = (randomHash >>> segmentShift) & segmentMask;
        } else {
            segmentStart = (hash(keyHint.hashCode()) >>> segmentShift) & segmentMask;
        }

        int segmentIndex = segmentStart;
        do {
            final HashEntry<Object, Element>[] table = segments[segmentIndex].table;
            final int tableStart = randomHash & (table.length - 1);
            int tableIndex = tableStart;
            do {
                for (HashEntry<Object, Element> e = table[tableIndex]; e != null; e = e.next) {
                    Element value = e.value;
                    if (value != null) {
                        sampled.add(value);
                    }
                }

                if (sampled.size() >= size) {
                    return sampled.toArray(new Element[sampled.size()]);
                }

                //move to next table slot
                tableIndex = (tableIndex + 1) & (table.length - 1);
            } while (tableIndex != tableStart);

            //move to next segment
            segmentIndex = (segmentIndex + 1) & segmentMask;
        } while (segmentIndex != segmentStart);

        return sampled.toArray(new Element[sampled.size()]);
    }


    /**
     * Returns the number of key-value mappings in this map without locking anything.
     * This may not give the exact element count as locking is avoided.
     * If the map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    public int quickSize() {
        long size = 0;

        for (Segment<Object, Element> segment : this.segments) {
            size += segment.count;
        }

        if (size > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        return (int) size;
    }

}
