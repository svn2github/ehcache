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

package net.sf.ehcache.store;

import java.util.Random;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Test logic for initialCapacity for MemoryStore
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class InitialCapacityTest extends TestCase {

    private static final Random random = new Random(System.currentTimeMillis());

    /**
     * Test the initial capacity logic
     */
    @Test
    public void testInitialCapacity() {
        float loadFactor = MemoryStore.DEFAULT_LOAD_FACTOR;
        // test positive sizes
        for (int i = 0; i < 1000; i++) {
            int n = random.nextInt(Integer.MAX_VALUE);
            assertTrue(n >= 0);
            doTestInitialCapacity((int) Math.ceil(n / loadFactor), loadFactor, n);
        }
        // test negative sizes
        for (int i = 0; i < 1000; i++) {
            int n = random.nextInt(Integer.MAX_VALUE) * -1;
            assertTrue(n <= 0);
            // initialCapacity is zero for negative maximumSizes
            doTestInitialCapacity(0, loadFactor, n);
        }
        // test 0
        doTestInitialCapacity(0, loadFactor, 0);

        // test Integer.MAX_VALUE
        doTestInitialCapacity(Integer.MAX_VALUE, loadFactor, Integer.MAX_VALUE);
    }

    private void doTestInitialCapacity(int expectedValue, float loadFactor, int maximumSizeGoal) {
        assertEquals(expectedValue, MemoryStore.getInitialCapacityForLoadFactor(maximumSizeGoal, loadFactor));
    }

}
