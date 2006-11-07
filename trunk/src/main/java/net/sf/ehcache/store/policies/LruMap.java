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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A map to encaupsulate the LRU eviction policy
 * 
 * @author Jody Brownell
 * @since 1.2.4
 * @version $Id$
 */
public class LruMap extends LinkedHashMap implements PolicyMap {
    private static final int INITIAL_CAPACITY = 100;
    private static final float GROWTH_FACTOR = .75F;

    /**
     * Default constructor
     */
    public LruMap() {
        // create this map to use the access order
        this(true);
    }

    /**
     * Construct an LruMap indicating the order policy
     * @param  accessOrder     the ordering mode - <tt>true</tt> for
     *         access-order, <tt>false</tt> for insertion-order.
     */
    LruMap(boolean accessOrder) {
        super(INITIAL_CAPACITY, GROWTH_FACTOR, accessOrder);
    }

    /**
     * @see net.sf.ehcache.store.policies.PolicyMap#findElementToEvict(net.sf.ehcache.Element)
     * @param element
     */
    public synchronized Map.Entry findElementToEvict(Element element) {
        Map.Entry entry = null;
        Iterator i = this.entrySet().iterator();
        if (i.hasNext()) {
            entry = (Map.Entry) i.next();
        }

        return entry;
    }
}
