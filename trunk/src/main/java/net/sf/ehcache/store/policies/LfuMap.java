/**
 *  Copyright 2003-2006 Greg Luck
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
package net.sf.ehcache.store.policies;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A map to encaupsulate the LFU eviction policy. This adapted from LfuMemoryStore.
 * Less Frequently Used (LFU) implementation of the policy map. Actually keeping track of the least used, then the
 * second least and so on is very expensive, 3 or 4 orders of magnitude more expensive than the others policies. Costs are
 * incurred on put, get and remove.
 * <p/>
 * Instead this implementation does not quarantee that
 * the element removed will actually be the least used. Rather, it lets you make statements about confidence intervals
 * against the likelihood that an element is in some lowest percentile of the hit count distribution. Costs are only
 * incurred on overflow when an element to be evicted must be selected.
 * <p/>
 * For those with a statistical background the branch of stats which deals with this is hypothesis testing and
 * the Student's T distribution. The higher your sample the greater confidence you can have in a hypothesis, in
 * this case whether or not the "lowest" value lies in the bottom half or quarter of the distribution. Adding
 * samples rapidly increases confidence but the return from extra sampling rapidly diminishes.
 * <p/>
 * Cost is not affected much by sample size, indicating it is probably the iteration that is causing most of the
 * time. If we had access to the array backing Map, all would work very fast.
 * <p/>
 * A 99.99% confidence interval can be achieved that the "lowest" element is actually in the bottom quarter of the
 * hit count distribution with a sample size of 30, which is the default.
 *
 * @author Greg Luck (original LfuMemoryStore)
 * @author Jody Brownell
 * @version $Id$
 * @since 1.2.4
 */
public class LfuMap extends HashMap implements PolicyMap {

    private static final Log LOG = LogFactory.getLog(LfuMap.class.getName());

    private static final int DEFAULT_SAMPLE_SIZE = 30;

    private final Random random = new Random();

    /**
     * @param elementJustAdded the element just added which should be exempt from eviction
     * @see PolicyMap#findElementToEvict(net.sf.ehcache.Element)
     */
    public Map.Entry findElementToEvict(Element elementJustAdded) {
        Map.Entry[] elements = sampleElements(calculateSampleSize());
        return lowestElementFromArray(elements, elementJustAdded);
    }

    private int calculateSampleSize() {
        if (size() < DEFAULT_SAMPLE_SIZE) {
            return size();
        } else {
            return DEFAULT_SAMPLE_SIZE;
        }
    }

    /**
     * Uses random numbers to sample the entire map.
     *
     * @param sampleSize how many samples to take
     * @return an array of sampled elements
     */
    public final Map.Entry[] sampleElements(int sampleSize) {
        int[] offsets = generateRandomOffsets(sampleSize);
        Map.Entry[] elements = new Map.Entry[sampleSize];
        Iterator iterator = entrySet().iterator();
        for (int i = 0; i < sampleSize; i++) {
            for (int j = 0; j < offsets[i]; j++) {
                iterator.next();
            }
            elements[i] = (Map.Entry) iterator.next();
        }
        return elements;
    }

    private Map.Entry lowestElementFromArray(Map.Entry[] entries, Element elementJustAdded) {

        Map.Entry entryJustAdded = new net.sf.ehcache.store.policies.Entry(elementJustAdded.getObjectKey(), elementJustAdded);

        //edge condition when Memory Store configured to size 0
        if (entries.length == 1) {
            return entryJustAdded;
        }

        Map.Entry lowestEntry = null;
        for (int i = 0; i < entries.length; i++) {
            Map.Entry entry = entries[i];
            if (lowestEntry == null) {
                if (!entry.equals(entryJustAdded)) {
                    lowestEntry = entry;
                }
            } else {
                if (isNewEntryLower(entry, lowestEntry) && !entry.equals(entryJustAdded)) {
                    lowestEntry = entry;
                }
            }
        }
        return lowestEntry;
    }

    /**
     * Find the element with the lowest number of hits.
     *
     * @param lowestEntry the current lowest
     * @param newEntry       the new element to compare the existing element to
     * @return true, if the new element has less hits than the existing lowest
     */
    protected boolean isNewEntryLower(Map.Entry newEntry, Map.Entry lowestEntry) {
        Element lowest = (Element) lowestEntry.getValue();
        Element newElement = (Element) newEntry.getValue();
        return lowest.getHitCount() > newElement.getHitCount();
    }

    private int[] generateRandomOffsets(int sampleSize) {
        int size = size();
        int[] offsets = new int[sampleSize];
        int maxOffset = size / sampleSize;
        for (int i = 0; i < sampleSize; i++) {
            offsets[i] = random.nextInt(maxOffset);
        }
        return offsets;
    }

}
