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

import static org.terracotta.context.query.Matchers.allOf;
import static org.terracotta.context.query.Matchers.attributes;
import static org.terracotta.context.query.Matchers.context;
import static org.terracotta.context.query.Matchers.hasAttribute;
import static org.terracotta.context.query.Matchers.identifier;
import static org.terracotta.context.query.Matchers.subclassOf;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

import org.terracotta.context.query.Query;
import org.terracotta.context.query.QueryBuilder;
import org.terracotta.statistics.OperationStatistic;

/**
 * The Enum OperationType.
 *
 * @author cdennis
 */
 enum OperationType {

    /** The cache get. */
    CACHE_GET(CacheOperationOutcomes.GetOutcome.class, cache().chain(childStatistic("get").build()).ensureUnique().build()),

    /** The cache put. */
    CACHE_PUT(CacheOperationOutcomes.PutOutcome.class, cache().chain(childStatistic("put").build()).ensureUnique().build()),

    /** The cache remove. */
    CACHE_REMOVE(CacheOperationOutcomes.RemoveOutcome.class, cache().chain(childStatistic("remove").build()).ensureUnique().build()),

    /** The heap get. */
    HEAP_GET(StoreOperationOutcomes.GetOutcome.class, queryBuilder().empty().build()),

    /** The heap put. */
    HEAP_PUT(StoreOperationOutcomes.PutOutcome.class, queryBuilder().empty().build()),

    /** The heap remove. */
    HEAP_REMOVE(StoreOperationOutcomes.RemoveOutcome.class, queryBuilder().empty().build()),

    /** The offheap get. */
    OFFHEAP_GET(StoreOperationOutcomes.GetOutcome.class, queryBuilder().empty().build()),

    /** The offheap put. */
    OFFHEAP_PUT(StoreOperationOutcomes.PutOutcome.class, queryBuilder().empty().build()),

    /** The offheap remove. */
    OFFHEAP_REMOVE(StoreOperationOutcomes.RemoveOutcome.class, queryBuilder().empty().build()),

    /** The disk get. */
    DISK_GET(StoreOperationOutcomes.GetOutcome.class, queryBuilder().empty().build()),

    /** The disk put. */
    DISK_PUT(StoreOperationOutcomes.PutOutcome.class, queryBuilder().empty().build()),

    /** The disk remove. */
    DISK_REMOVE(StoreOperationOutcomes.RemoveOutcome.class, queryBuilder().empty().build()),

    /** The xa commit. */
    XA_COMMIT(XaCommitOutcome.class, queryBuilder().empty().build()),

    /** The xa rollback. */
    XA_ROLLBACK(XaRollbackOutcome.class, queryBuilder().empty().build()),

    /** The xa recovery. */
    XA_RECOVERY(XaRecoveryOutcome.class, queryBuilder().empty().build()),

    /** The search. */
    SEARCH(CacheOperationOutcomes.SearchOutcome.class, queryBuilder().empty().build()) {
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
    EVICTED(CacheOperationOutcomes.EvictionOutcome.class, queryBuilder().empty().build()),

    /** The expired. */
    EXPIRED(CacheOperationOutcomes.ExpiredOutcome.class, queryBuilder().empty().build());

    private final Class<? extends Enum> type;
    private final Query query;

    private OperationType(Class<? extends Enum> type, Query query) {
        this.type = type;
        this.query = query;
    }

    /**
     * Type.
     *
     * @return the class<? extends enum>
     */
    final Class<? extends Enum> type() {
        return type;
    }

    /**
     * Query.
     *
     * @return the query
     */
    Query query() {
        return query;
    }

    /**
     * History.
     *
     * @return the int
     */
    int history() {
        return 30;
    }

    /**
     * Interval.
     *
     * @return the long
     */
    long interval() {
        return 1;
    }

    /**
     * Window.
     *
     * @return the long
     */
    long window() {
        return 1;
    }

    /**
     * Cache.
     *
     * @return the query builder
     */
    static QueryBuilder cache() {
        return queryBuilder().children().filter(context(identifier(subclassOf(Cache.class))));
    }

    /**
     * Child statistic.
     *
     * @param name the name
     * @return the query builder
     */
    static QueryBuilder childStatistic(String name) {
        return queryBuilder().children().filter(context(allOf(identifier(subclassOf(OperationStatistic.class)), attributes(hasAttribute("name", name)))));
    }
}
