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

package net.sf.ehcache.util;


/**
 * A class that relies on the real world randomness of Element creation and updating time.
 * @author Greg Luck
 */
public class FastRandom {

    private static final long ONE_BYTE = 0x000000ff;
    private int cutOff;


    /**
     * Creates a new FastRandom object, with the specified probability. The least significant
     * byte, which changes most frequently, is used.
     * @param probability the desired probability. If 10%, then 10% of the time, +- a small
     * variance, a call to select will return true.
     */
    public FastRandom(float probability) {
        cutOff = (int) (probability * ONE_BYTE);
    }

    /**
     * Uses the probability to determine whether this value is selected.
     * This method always returns the same result for the same data.
     * <p/>
     * Any randomness is provided by the randomness of the times passed in.
     * @param randomTime repeated calls to this method must have random times passed
     */
    public boolean select(long randomTime) {

        long onlyLastEightBits = randomTime & ONE_BYTE;
        boolean result = onlyLastEightBits <= cutOff;
        return result;

    }

}

