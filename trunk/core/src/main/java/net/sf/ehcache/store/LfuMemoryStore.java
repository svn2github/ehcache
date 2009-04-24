/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.store;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.util.FastRandom;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Less Frequently Used (LFU) implementation of the memory store. Actually keeping track of the least used, then the
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
 * @author Greg Luck
 * @version $Id$
 */
public class LfuMemoryStore extends MemoryStore {

    private static final Logger LOG = Logger.getLogger(LfuMemoryStore.class.getName());

    /**
     * This number is magic. It was established using empirical testing of the two approaches
     * in CacheTest#testConcurrentReadWriteRemoveLFU. 5000 is the cross over point
     * between the two algorithms.
     */
    private static final int TOO_LARGE_TO_EFFICIENTLY_ITERATE = 5000;

    /**
     * 10% of the time updata data in our sample population
     */
    private static final float PUT_SAMPLING_PERCENTAGE = .10f;

    /**
     * This is the default from {@link ConcurrentHashMap}. It should never be used, because
     * we size the map to the max size of the store.
     */
    private static final float DEFAULT_LOAD_FACTOR = .75f;

    /**
     * Set optimisation for 100 concurrent threads.
     */
    private static final int CONCURRENCY_LEVEL = 100;

    private static final int ONE_THOUSAND = 1000;
    private static final int ONE_HUNDRED_THOUSAND = 100000;
    private static final int TWO_THOUSAND = 2000;

    private FastRandom fastRandom;
    private Policy policy;
    private AtomicReferenceArray<Object> keySample;
    private AtomicInteger keySamplePointer;
    private int size;

    private int sampleSize;
    private boolean useKeySample;

    /**
     * Constructor for the LfuMemoryStore object.
     *
     * @param cache     The cache which owns this store
     * @param diskStore The DiskStore referenced by this store, if any
     */
    protected LfuMemoryStore(Ehcache cache, Store diskStore) {
        super(cache, diskStore);
        policy = new LfuPolicy();
        size = cache.getCacheConfiguration().getMaxElementsInMemory();
        sampleSize = calculateKeySampleSize();
        map = new ConcurrentHashMap(size, DEFAULT_LOAD_FACTOR, CONCURRENCY_LEVEL);
        if (size > TOO_LARGE_TO_EFFICIENTLY_ITERATE) {
            useKeySample = true;
            keySample = new AtomicReferenceArray<Object>(sampleSize);
            keySamplePointer = new AtomicInteger(0);
            fastRandom = new FastRandom(PUT_SAMPLING_PERCENTAGE);
        }
    }

    /**
     * These numbers are not really based on any rigid analysis.
     */
    private int calculateKeySampleSize() {
        if (size < ONE_THOUSAND) {
            return size;
        } else if (size > ONE_THOUSAND && size < ONE_HUNDRED_THOUSAND) {
            return ONE_THOUSAND;
        } else {
            return TWO_THOUSAND;
        }
    }


    /**
     * Puts an element into the cache.
     */
    public final void doPut(Element elementJustAdded) {
        if (isFull()) {
            removeLfuElement(elementJustAdded);
        }
        if (useKeySample) {

            if (fastRandom.select(elementJustAdded.getLatestOfCreationAndUpdateTime())) {
                saveKey(elementJustAdded);
            }
        }

    }

    private void saveKey(Element elementJustAdded) {
        int index = incrementIndex();
        Object key = keySample.get(index);
        Element oldElement = null;
        if (key != null) {
            oldElement = (Element) map.get(key);
        }
        if (oldElement != null) {
            if (policy.compare(oldElement, elementJustAdded)) {
                //new one will always be more desirable for eviction as no gets yet, unless no gets on old one.
                //Consequence of this algorithm
                //new one more desirable for eviction so save it
                keySample.set(index, elementJustAdded.getObjectKey());
            }
        } else {
            keySample.set(index, elementJustAdded.getObjectKey());
        }

    }

    /**
     * A safe incrementer, which loops back to zero when it exceeds the array size
     */
    private int incrementIndex() {
        int index = keySamplePointer.getAndIncrement();
        if (index > (keySample.length() - 1)) {
            keySamplePointer.set(0);
            return 0;
        } else {
            return index;
        }
    }


    private void removeLfuElement(Element elementJustAdded) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Cache is full. Removing LFU element ...");
        }

        // First element of the sorted list is the candidate for the removal
        Element element = findRelativelyUnused(elementJustAdded);
        //this CAN happen rarely. Let the store get one bigger
        if (element == null) {
            return;
        }

        // If the element is expired remove
        if (element.isExpired()) {
            remove(element.getObjectKey());
            notifyExpiry(element);
            return;
        }

        evict(element);
        remove(element.getObjectKey());
    }

//    /**
//     * Find a "relatively" unused element, but not the element just added.
//     */
//    final Element findRelativelyUnused(Element elementJustAdded) {
//        Element[] elements = sampleElements(map.size());
//        //can be null. Let the cache get bigger by one.
//        return policy.selectedBasedOnPolicy(elements, elementJustAdded);
//    }

    /**
     * Find a "relatively" unused element, but not the element just added.
     */
    final Element findRelativelyUnused(Element elementJustAdded) {
        Element[] elements;
        if (useKeySample) {
            elements = chooseElementsFromPopulationSample();
            //this can return null. Let the cache get bigger by one.
            return policy.selectedBasedOnPolicy(elements, elementJustAdded);
        } else {
            //+1 because element was added
            elements = sampleElements(map.size());
            //this can return null. Let the cache get bigger by one.
            return policy.selectedBasedOnPolicy(elements, elementJustAdded);
        }
    }

    /**
     * Uses a random sample from the population sample.
     *
     * @return an array of sampled elements
     */
    Element[] chooseElementsFromPopulationSample() {
        int[] indices = LfuPolicy.generateRandomSampleIndices(sampleSize);
        Element[] elements = new Element[indices.length];
        for (int i = 0; i < indices.length; i++) {
            Object key = keySample.get(indices[i]);
            if (key == null) {
                continue;
            }
            elements[i] = (Element) map.get(key);
        }
        return elements;
    }

    /**
     * Uses random numbers to sample the entire map.
     *
     * @return an array of sampled elements
     */
    Element[] sampleElements(int size) {
        int[] offsets = LfuPolicy.generateRandomSample(size);
        Element[] elements = new Element[offsets.length];
        Iterator iterator = map.values().iterator();
        for (int i = 0; i < offsets.length; i++) {
            for (int j = 0; j < offsets[i]; j++) {
                //fast forward
                try {
                    iterator.next();
                } catch (NoSuchElementException e) {
                    //e.printStackTrace();
                }
            }

            try {
                elements[i] = ((Element) iterator.next());
            } catch (NoSuchElementException e) {
                //e.printStackTrace();
            }
        }
        return elements;
    }


}





