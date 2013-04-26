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

package net.sf.ehcache;

/**
 * Interface containing cache operation outcome enums.
 *
 * @author cschanck
 */
public interface CacheOperationOutcomes {

    /**
     * Outcomes for cache Get operations.
     *
     * @author cschanck
     */
    enum GetOutcome {
        /** hit. */
        HIT,
        /** miss expired. */
        MISS_EXPIRED,
        /** miss not found. */
        MISS_NOT_FOUND
    };

    /**
     * The outcomes for Put Outcomes.
     */
    enum PutOutcome {
        /** added. */
        ADDED,
        /** updated. */
        UPDATED,

        /** ignored. */
        IGNORED
    };

    /**
     * The outcomes for remove operations.
     */
    enum RemoveOutcome {
        /** success. */
        SUCCESS
    };

    /**
     * The outcomes for GetAll operations.
     */
    enum GetAllOutcome {
        /** all miss. */
        ALL_MISS,
        /** all hit. */
        ALL_HIT,
        /** partial. */
        PARTIAL
    };

    /**
     * The outcomes for GetAll operations.
     */
    enum PutAllOutcome {

        /** The ignored. */
        IGNORED,
        /** The completed. */
        COMPLETED
    };

    /**
     * The outcomes for GetAll operations.
     */
    enum RemoveAllOutcome {

        /** The ignored. */
        IGNORED,
        /** The completed. */
        COMPLETED
    };

    /**
     * The outcomes for the store search operation.
     */
    enum SearchOutcome {
        /** success. */
        SUCCESS,
        /** exception. */
        EXCEPTION
    };

    /**
     * The eviction outcomes.
     */
    enum EvictionOutcome {
        /** success. */
        SUCCESS
    };

    /**
     * The expiration outcomes.
     */
    enum ExpiredOutcome {
        /** success. */
        SUCCESS,
        /** The failure. */
        FAILURE
    };
    
    /**
     * Cluster event operation outcomes.
     * 
     * @author cschanck
     *
     */
    public static enum ClusterEventOutcomes {
        
        /** offline. */
        OFFLINE,
        
        /** online. */
        ONLINE, 
        
        /** rejoin. */
        REJOINED
    };
    
    /**
     * The Enum NonStopOperationOutcomes.
     */
    public static enum NonStopOperationOutcomes {
        
        /** The success. */
        SUCCESS,
        
        /** failure */
        FAILURE,
        
        /** The rejoin driven timeout */
        REJOIN_TIMEOUT,
        
        /** The timeout. */
        TIMEOUT;
    }
}
