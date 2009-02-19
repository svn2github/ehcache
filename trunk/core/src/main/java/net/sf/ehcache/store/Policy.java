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

import net.sf.ehcache.Element;

import java.util.Random;

/**
 * A base policy class
 *
 * @author Greg Luck
 */
public abstract class Policy {


    /**
     * The sample size to use
     */
    static final int DEFAULT_SAMPLE_SIZE = 30;

    /**
     * Used to select random numbers
     */
    static final Random RANDOM = new Random();

    /**
     * sampleSize how many samples to take
     * @param populationSize the size of the store
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
    public abstract Element selectedBasedOnPolicy(Element[] sampledElements, Element justAdded);

    /**
     * Generates a random sample from a population
     * @param populationSize the size to draw from
     * @return a list of random offsets
     */
    public static int[] generateRandomSample(int populationSize) {
        int sampleSize = calculateSampleSize(populationSize);
        int[] offsets = new int[sampleSize];
        int maxOffset = populationSize / sampleSize;
        for (int i = 0; i < sampleSize; i++) {
            offsets[i] = RANDOM.nextInt(maxOffset);
        }
        return offsets;
    }
}