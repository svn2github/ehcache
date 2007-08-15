/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

import java.util.Random;

/**
 * Contains common LFU policy code for use between the LfuMemoryStore and the DiskStore, which also
 * uses an LfuPolicy for evictions.
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class LfuPolicy {

    private static final int DEFAULT_SAMPLE_SIZE = 30;

    private static final Random RANDOM = new Random();

    /**
     * Utilitiy class therefore no constructor
     */
    private LfuPolicy() {
    }

    /**
     * sampleSize how many samples to take
     * @return the smaller of the map size and the default sample size of 30
     */
    private static int calculateSampleSize(int populationSize) {
        if (populationSize < DEFAULT_SAMPLE_SIZE) {
            return populationSize;
        } else {
            return DEFAULT_SAMPLE_SIZE;
        }

    }


    /**
     * Finds the least hit of the sampled elements provided
     * @param sampledElements this should be a random subset of the population
     * @param justAdded we never want to select the element just added. May be null.
     * @return the least hit
     */
    public static LfuPolicy.Metadata leastHit(LfuPolicy.Metadata[] sampledElements, LfuPolicy.Metadata justAdded) {
        //edge condition when Memory Store configured to size 0
        if (sampledElements.length == 1 && justAdded != null) {
            return justAdded;
        }
        LfuPolicy.Metadata lowestElement = null;
        for (int i = 0; i < sampledElements.length; i++) {
            LfuPolicy.Metadata element = sampledElements[i];
            if (lowestElement == null) {
                if (!element.equals(justAdded)) {
                    lowestElement = element;
                }
            } else {
                if (element.getHitCount() < lowestElement.getHitCount() && !element.equals(justAdded)) {
                    lowestElement = element;
                }
            }
        }
        return lowestElement;
    }

    /**
     * Generates a random sample from a population
     * @param populationSize the size to draw from
     */
    public static int[] generateRandomSample(int populationSize) {
        int sampleSize = LfuPolicy.calculateSampleSize(populationSize);
        int[] offsets = new int[sampleSize];
        int maxOffset = populationSize / sampleSize;
        for (int i = 0; i < sampleSize; i++) {
            offsets[i] = RANDOM.nextInt(maxOffset);
        }
        return offsets;
    }


    /**
     * A type representing relevant metadata from an element, used by LfuPolicy for its operations.
     */
    public static interface Metadata {

        /**
         * @return the key of this object
         */
        Object getObjectKey();

        /**
         *
         * @return the hit count for the element
         */
        long getHitCount();

    }
}
