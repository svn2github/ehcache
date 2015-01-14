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
 * Used to compare two element values.
 * Implementations must define a constructor accepting a single CacheConfiguration argument.
 *
 * @author Ludovic Orban
 */
public interface ElementValueComparator {

    /**
     * Compare the two elements.
     * Null values have to be supported.
     *
     * @param e1 element to compare
     * @param e2 element to compare
     * @return {@code true} if the elements are equal
     */
    boolean equals(Element e1, Element e2);

}
