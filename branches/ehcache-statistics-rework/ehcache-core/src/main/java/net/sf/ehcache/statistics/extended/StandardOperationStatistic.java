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

package net.sf.ehcache.statistics.extended;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

/**
 * The Enum OperationType.
 *
 * @author cdennis
 */
enum StandardOperationStatistic {

    /** The cache get. */
    CACHE_GET(true, CacheOperationOutcomes.GetOutcome.class, "get", "cache"),
    /** The cache put. */
    CACHE_PUT(true, CacheOperationOutcomes.PutOutcome.class, "put", "cache"),
    /** The cache remove. */
    CACHE_REMOVE(true, CacheOperationOutcomes.RemoveOutcome.class, "remove", "cache"),

    /** The heap get. */
    HEAP_GET(true, StoreOperationOutcomes.GetOutcome.class, "get", "local-heap"),

    /** The heap put. */
    HEAP_PUT(true, StoreOperationOutcomes.PutOutcome.class, "put", "local-heap"),

    /** The heap remove. */
    HEAP_REMOVE(true, StoreOperationOutcomes.RemoveOutcome.class, "remove", "local-heap"),

    /** The offheap get. */
    OFFHEAP_GET(StoreOperationOutcomes.GetOutcome.class, "get", "local-offheap"),

    /** The offheap put. */
    OFFHEAP_PUT(StoreOperationOutcomes.PutOutcome.class, "put", "local-offheap"),

    /** The offheap remove. */
    OFFHEAP_REMOVE(StoreOperationOutcomes.RemoveOutcome.class, "remove", "local-offheap"),

    /** The disk get. */
    DISK_GET(StoreOperationOutcomes.GetOutcome.class, "get", "local-disk"),

    /** The disk put. */
    DISK_PUT(StoreOperationOutcomes.PutOutcome.class, "put", "local-disk"),

    /** The disk remove. */
    DISK_REMOVE(StoreOperationOutcomes.RemoveOutcome.class, "remove", "local-disk"),

    /** The xa commit. */
    XA_COMMIT(XaCommitOutcome.class, "xa-commit", "xa-transactional"),

    /** The xa rollback. */
    XA_ROLLBACK(XaRollbackOutcome.class, "xa-rollback", "xa-transactional"),

    /** The xa recovery. */
    XA_RECOVERY(XaRecoveryOutcome.class, "xa-recovery", "xa-transactional"),

    /** The search. */
    SEARCH(true, CacheOperationOutcomes.SearchOutcome.class, "search", "cache") {
        @Override
        long interval() {
            return 10;
        }

        @Override
        long window() {
            return 10;
        }
    },

    /** The evicted. */
    EVICTION(CacheOperationOutcomes.EvictionOutcome.class, "eviction"),

    /** The expired. */
    EXPIRY(true, CacheOperationOutcomes.ExpiredOutcome.class, "expiry");

    private final boolean required;
    private final Class<? extends Enum> type;
    private final String name;
    private final Set<String> tags;

    private StandardOperationStatistic(Class<? extends Enum> type, String name, String ... tags) {
        this(false, type, name, tags);
    }
    
    private StandardOperationStatistic(boolean required, Class<? extends Enum> type, String name, String ... tags) {
        this.required = required;
        this.type = type;
        this.name = name;
        this.tags = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(tags)));
    }

    /**
     * If this statistic is required.
     * <p>
     * If required and this statistic is not present an exception will be thrown.
     * 
     * @return 
     */
    final boolean required() {
        return required;
    }
    
    /**
     * Operation result type.
     *
     * @return operation result type
     */
    final Class<? extends Enum> type() {
        return type;
    }

    /**
     * The name of the statistic as found in the statistics context tree.
     * 
     * @return the statistic name
     */
    final String operationName() {
        return name;
    }
    
    /**
     * A set of tags that will be on the statistic found in the statistics context tree.
     * 
     * @return the statistic tags
     */
    final Set<String> tags() {
        return tags;
    }

    /**
     * The default size of the kept history sample.
     *
     * @return the default history size
     */
    int history() {
        return 30;
    }

    /**
     * The period in seconds at which to sample for the history.
     *
     * @return the history sample period
     */
    long interval() {
        return 1;
    }

    /**
     * The size in seconds of the window for rates and latencies.
     *
     * @return the window size
     */
    long window() {
        return 1;
    }
}
