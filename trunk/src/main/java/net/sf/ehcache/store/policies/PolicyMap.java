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

import net.sf.ehcache.Element;

import java.util.Map;

/**
 * An interface to encapsulate store eviction policies using Map semantics
 *
 * @author Jody Brownell
 * @author Greg Luck
 * @version $Id$
 * @since 1.2.4
 */
public interface PolicyMap extends Map {
    /**
     * Find a Map.Entry which is the most eligible entry according to the eviction policy
     * @param elementJustAdded the element that was just added to the store. We do not want
     * to evict this unless the store is set to 0 size.
     */
    public Map.Entry findElementToEvict(Element elementJustAdded);
}