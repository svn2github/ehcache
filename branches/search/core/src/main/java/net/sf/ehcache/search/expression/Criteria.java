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

package net.sf.ehcache.search.expression;

import net.sf.ehcache.store.ElementAttributeValues;

/**
 * Criteria interface defines a boolean function that computes a search match result
 *
 * @author teck
 */
public interface Criteria {

    /**
     * Test this criteria against a cache element
     *
     * @param attributeValues accessor for attributes values on the current element this critetia executed against
     * @return true if the criteria matches this element
     */
    boolean execute(ElementAttributeValues attributeValues);

}
