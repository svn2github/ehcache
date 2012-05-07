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

package net.sf.ehcache.config;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

/**
 * Class to hold the SizeOf policy configuration.
 *
 * @author Ludovic Orban
 */
public class SizeOfPolicyConfiguration implements Cloneable {
    /**
     * Default max traversal depth
     */
    public static final int DEFAULT_MAX_SIZEOF_DEPTH = 1000;
    /**
     * Default max traversal depth exceeded behavior
     */
    public static final MaxDepthExceededBehavior DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR = MaxDepthExceededBehavior.CONTINUE;

    /**
     * Enum of the possible behaviors of the SizeOf engine when the max depth is exceeded
     */
    public static enum MaxDepthExceededBehavior {
        /**
         * Abort the SizeOf engine's traversal and immediately return the partially calculated size
         */
        ABORT,

        /**
         * Warn about the exceeded max depth but continue traversal of the sized element
         */
        CONTINUE;

        /**
         * Returns true if this behavior is equal to ABORT
         *
         * @return true if this behavior is equal to ABORT
         */
        public boolean isAbort() {
            return this == ABORT;
        }

        /**
         * Returns true if this behavior is equal to CONTINUE
         *
         * @return true if this behavior is equal to CONTINUE
         */
        public boolean isContinue() {
            return this == CONTINUE;
        }
    }

    private volatile int maxDepth = DEFAULT_MAX_SIZEOF_DEPTH;
    private volatile MaxDepthExceededBehavior maxDepthExceededBehavior = DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR;


    /**
     * Gets the maximum depth the SizeOf engine can normally traverse
     *
     * @return the maximum depth the SizeOf engine can normally traverse
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * Sets the maximum depth the SizeOf engine can normally traverse
     *
     * @param maxDepth the maximum depth the SizeOf engine can normally traverse
     */
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Builder method to set the maximum depth the SizeOf engine can normally traverse
     *
     * @param maxDepth the maximum depth the SizeOf engine can normally traverse
     * @return this SizeOfPolicyConfiguration object
     */
    public SizeOfPolicyConfiguration maxDepth(int maxDepth) {
        setMaxDepth(maxDepth);
        return this;
    }

    /**
     * Gets the behavior of the SizeOf engine when the max depth is reached
     *
     * @return the behavior of the SizeOf engine when the max depth is reached
     */
    public MaxDepthExceededBehavior getMaxDepthExceededBehavior() {
        return maxDepthExceededBehavior;
    }

    /**
     * Sets the behavior of the SizeOf engine when the max depth is reached
     *
     * @param maxDepthExceededBehavior the behavior of the SizeOf engine when the max depth is reached
     */
    public void setMaxDepthExceededBehavior(String maxDepthExceededBehavior) {
        if (maxDepthExceededBehavior == null) {
            throw new IllegalArgumentException("maxDepthExceededBehavior must be non-null");
        }
        this.maxDepthExceededBehavior(MaxDepthExceededBehavior.valueOf(MaxDepthExceededBehavior.class, maxDepthExceededBehavior.toUpperCase()));
    }

    /**
     * Builder method to set the behavior of the SizeOf engine when the max depth is reached
     *
     * @param maxDepthExceededBehavior the behavior of the SizeOf engine when the max depth is reached
     * @return this SizeOfPolicyConfiguration object
     */
    public SizeOfPolicyConfiguration maxDepthExceededBehavior(MaxDepthExceededBehavior maxDepthExceededBehavior) {
        this.maxDepthExceededBehavior = maxDepthExceededBehavior;
        return this;
    }

    /**
     * Builder method to set the behavior of the SizeOf engine when the max depth is reached using a String object
     *
     * @param maxDepthExceededBehavior the behavior of the SizeOf engine when the max depth is reached
     * @return this SizeOfPolicyConfiguration object
     */
    public SizeOfPolicyConfiguration maxDepthExceededBehavior(String maxDepthExceededBehavior) {
        setMaxDepthExceededBehavior(maxDepthExceededBehavior);
        return this;
    }

    /**
     * Helper method which resolves the max depth of a cache, using the cache manager's one if none was configured
     * on the cache itself.
     *
     * @param cache the cache from which to resolve the max depth
     * @return the resolved max depth
     */
    public static int resolveMaxDepth(Ehcache cache) {
        if (cache == null) {
            return DEFAULT_MAX_SIZEOF_DEPTH;
        }
        CacheManager cacheManager = cache.getCacheManager();
        return resolvePolicy(cacheManager == null ? null : cacheManager.getConfiguration(), cache.getCacheConfiguration()).getMaxDepth();
    }

    /**
     * Helper method which resolves the MaxDepthExceededBehavior of a cache, using the cache manager's one if none was configured
     * on the cache itself.
     *
     * @param cache the cache from which to resolve the MaxDepthExceededBehavior
     * @return the resolved MaxDepthExceededBehavior
     */
    public static MaxDepthExceededBehavior resolveBehavior(Ehcache cache) {
        if (cache == null) {
            return DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR;
        }
        CacheManager cacheManager = cache.getCacheManager();
        if (cacheManager == null) {
            return resolvePolicy(null, cache.getCacheConfiguration()).getMaxDepthExceededBehavior();
        } else {
            return resolvePolicy(cacheManager.getConfiguration(), cache.getCacheConfiguration()).getMaxDepthExceededBehavior();
        }
    }

    private static SizeOfPolicyConfiguration resolvePolicy(Configuration configuration, CacheConfiguration cacheConfiguration) {
        SizeOfPolicyConfiguration sizeOfPolicyConfiguration = null;
        if (cacheConfiguration != null) {
            sizeOfPolicyConfiguration = cacheConfiguration.getSizeOfPolicyConfiguration();
        }
        if (sizeOfPolicyConfiguration == null) {
            if (configuration != null) {
                sizeOfPolicyConfiguration = configuration.getSizeOfPolicyConfiguration();
            } else {
                sizeOfPolicyConfiguration = new SizeOfPolicyConfiguration();
            }
        }

        return sizeOfPolicyConfiguration;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + maxDepth;
        result = prime * result + ((maxDepthExceededBehavior == null) ? 0 : maxDepthExceededBehavior.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SizeOfPolicyConfiguration other = (SizeOfPolicyConfiguration) obj;
        return (maxDepth == other.maxDepth && maxDepthExceededBehavior == other.maxDepthExceededBehavior);
    }
}
