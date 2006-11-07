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

package net.sf.ehcache.store;

import net.sf.ehcache.store.policies.FifoMap;
import net.sf.ehcache.store.policies.LfuMap;
import net.sf.ehcache.store.policies.LruMap;
import net.sf.ehcache.store.policies.PolicyMap;
import net.sf.ehcache.CacheException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A typesafe enumeration of eviction policies.
 * The policy used to evict elements from the {@link net.sf.ehcache.store.MemoryStore}.
 * This can be one of:
 * <ol>
 * <li>LRU - least recently used
 * <li>LFU - least frequently used
 * <li>FIFO - first in first out, the oldest element by creation time
 * </ol>
 * The default value is LRU
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 * @since 1.2
 */
public final class EvictionPolicy {

    /**
     * LRU - least recently used.
     */
    public static final EvictionPolicy LRU = new EvictionPolicy("LRU");

    /**
     * LFU - least frequently used.
     */
    public static final EvictionPolicy LFU = new EvictionPolicy("LFU");

    /**
     * FIFO - first in first out, the oldest element by creation time.
     */
    public static final EvictionPolicy FIFO = new EvictionPolicy("FIFO");

    private static final Log LOG = LogFactory.getLog(EvictionPolicy.class.getName());

    // for debug only
    private final String myName;

    /**
     * This class should not be subclassed or have instances created.
     * @param policy
     */
    private EvictionPolicy(String policy) {
        myName = policy;
    }

    /**
     * @return a String representation of the policy
     */
    public String toString() {
        return myName;
    }

    /**
     * Converts a string representation of the policy into a policy.
     *
     * @param policy either LRU, LFU or FIFO
     * @return one of the static instances
     */
    public static EvictionPolicy fromString(String policy) {
        if (policy != null) {
            if (policy.equalsIgnoreCase("LRU")) {
                return LRU;
            } else if (policy.equalsIgnoreCase("LFU")) {
                return LFU;
            } else if (policy.equalsIgnoreCase("FIFO")) {
                return FIFO;
            }
        }

        if (LOG.isWarnEnabled()) {
            LOG.warn("The evictionPolicy of " + policy + " cannot be resolved. The policy will be set to LRU");
        }
        return LRU;
    }

    /**
     * Create an instance of the map which implements the semantics of the policy 
     */
    public PolicyMap createPolicyMap() {
        if (this == LRU) {
            return new LruMap();
        } else if (this == FIFO) {
            return new FifoMap();
        } else if (this == LFU) {
            return new LfuMap();
        } else {
            throw new CacheException("Illegal policy map");
        }
    }
}
