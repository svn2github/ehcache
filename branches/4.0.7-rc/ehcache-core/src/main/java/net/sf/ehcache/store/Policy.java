/**
 *  Copyright Terracotta, Inc.
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

/**
 * An eviction policy.
 * <p/>
 * The Cache will use a policy at startup. There are three policy implementations provided in ehcache:
 * LRU, LFU and FIFO. However many other policies are possible. That the policy
 * has access to the whole element enables policies based on the key, value, metadata, statistics, or a combination of
 * any of the above.
 *
 * @author Greg Luck
 */
public interface Policy {

    /**
     * @return the name of the Policy. Inbuilt examples are LRU, LFU and FIFO.
     */
    String getName();

    /**
     * Finds the best eviction candidate based on the sampled elements. What distinguishes
     * this approach from the classic data structures approach is that an Element contains
     * metadata (e.g. usage statistics) which can be used for making policy decisions,
     * while generic data structures do not. It is expected that implementations will take
     * advantage of that metadata.
     *
     * @param sampledElements this should be a random subset of the population
     * @param justAdded       we probably never want to select the element just added.
     * It is provided so that it can be ignored if selected. May be null.
     * @return the selected Element
     */
    Element selectedBasedOnPolicy(Element[] sampledElements, Element justAdded);

    /**
     * Compares the desirableness for eviction of two elements
     *
     * @param element1 the element to compare against
     * @param element2 the element to compare
     * @return true if the second element is preferable for eviction to the first element
     * under ths policy
     */
    boolean compare(Element element1, Element element2);
}
