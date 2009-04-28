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

/**
 * An eviction policy.
 * todo work out how to let people set their own
 *
 * @author Greg Luck
 */
public interface Policy {

    /**
     * Finds the best eviction candidate based on the sampled elements. What distuingishes this approach
     * from the classic data structures approach is that an Element contains metadata (e.g. usage statistics)
     * which can be used for making policy decisions, while generic data structures do not. It is expected that
     *  implementations will take advantage of that metadata.
     *
     * @param sampledElements this should be a random subset of the population
     * @param justAdded       we never want to select the element just added. May be null.
     * @return the least hit
     */
    Element selectedBasedOnPolicy(Element[] sampledElements, Element justAdded);

    /**
     * Compares the desirableness for eviction of two elements
     *
     * @param element1 the element to compare against
     * @param element2 the element to compare
     * @return true if the second element is preferable for eviction to the first element under ths policy
     */
    boolean compare(Element element1, Element element2);
}
