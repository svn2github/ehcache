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

package net.sf.ehcache.constructs.nonstop;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.nonstop.behavior.ExceptionOnTimeoutBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.NoOpOnTimeoutBehavior;

/**
 * Enum encapsulating different CacheBehavior's used by TimeoutCache
 * 
 * @author Abhishek Sanoujam
 * 
 */
public enum NonStopCacheBehaviorType {

    /**
     * {@link CacheBehavior} which throws exception for all timed out operations
     */
    EXCEPTION_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonStopCacheBehavior newCacheBehavior(final Ehcache ehcache) {
            return ExceptionOnTimeoutBehavior.getInstance();
        }

    },
    /**
     * {@link CacheBehavior} which returns null for get operations and does nothing for put's and remove's
     */
    NULL_OP_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonStopCacheBehavior newCacheBehavior(final Ehcache ehcache) {
            return NoOpOnTimeoutBehavior.getInstance();
        }

    },
    /**
     * {@link CacheBehavior} which returns whatever local value is associated with the key for get operations and does nothing for put's
     * and remove's. Note that if the cache is clustered (e.g. with Terracotta), and the current node is not part of the cluster, it
     * may return stale/dirty values
     */
    LOCAL_READS_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonStopCacheBehavior newCacheBehavior(final Ehcache ehcache) {
            throw new UnsupportedOperationException("This feature is not yet implemented");
        }

    };

    /**
     * Creates and returns new instance of {@link NonStopCacheBehavior} for this type
     * 
     * @param ehcache
     * @return new instance of {@link NonStopCacheBehavior} for this type
     */
    public abstract NonStopCacheBehavior newCacheBehavior(Ehcache ehcache);
}
