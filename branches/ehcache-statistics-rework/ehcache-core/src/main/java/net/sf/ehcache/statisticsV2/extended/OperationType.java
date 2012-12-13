/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;
import org.terracotta.context.query.Query;

import static org.terracotta.context.query.Matchers.*;
import org.terracotta.context.query.QueryBuilder;
import static org.terracotta.context.query.QueryBuilder.*;
import org.terracotta.statistics.OperationStatistic;

/**
 *
 * @author cdennis
 */
 enum OperationType {
    CACHE_GET(CacheOperationOutcomes.GetOutcome.class, cache().chain(childStatistic("get").build()).ensureUnique().build()),
    CACHE_PUT(CacheOperationOutcomes.PutOutcome.class, cache().chain(childStatistic("put").build()).ensureUnique().build()),
    CACHE_REMOVE(CacheOperationOutcomes.RemoveOutcome.class, cache().chain(childStatistic("remove").build()).ensureUnique().build()),

    HEAP_GET(StoreOperationOutcomes.GetOutcome.class, queryBuilder().empty().build()),
    HEAP_PUT(StoreOperationOutcomes.PutOutcome.class, queryBuilder().empty().build()),
    HEAP_REMOVE(StoreOperationOutcomes.RemoveOutcome.class, queryBuilder().empty().build()),

    OFFHEAP_GET(StoreOperationOutcomes.GetOutcome.class, queryBuilder().empty().build()),
    OFFHEAP_PUT(StoreOperationOutcomes.PutOutcome.class, queryBuilder().empty().build()),
    OFFHEAP_REMOVE(StoreOperationOutcomes.RemoveOutcome.class, queryBuilder().empty().build()),

    DISK_GET(StoreOperationOutcomes.GetOutcome.class, queryBuilder().empty().build()),
    DISK_PUT(StoreOperationOutcomes.PutOutcome.class, queryBuilder().empty().build()),
    DISK_REMOVE(StoreOperationOutcomes.RemoveOutcome.class, queryBuilder().empty().build()),

    XA_COMMIT(XaCommitOutcome.class, queryBuilder().empty().build()),
    XA_ROLLBACK(XaRollbackOutcome.class, queryBuilder().empty().build()),
    XA_RECOVERY(XaRecoveryOutcome.class, queryBuilder().empty().build()),

    SEARCH(CacheOperationOutcomes.SearchOutcome.class, queryBuilder().empty().build()) {
        @Override
        long interval() {
            return 10;
        }
    },

    EVICTED(CacheOperationOutcomes.EvictionOutcome.class, queryBuilder().empty().build()),
    EXPIRED(CacheOperationOutcomes.ExpiredOutcome.class, queryBuilder().empty().build());

    private final Class<? extends Enum> type;
    private final Query query;

    private OperationType(Class<? extends Enum> type, Query query) {
        this.type = type;
        this.query = query;
    }

    final Class<? extends Enum> type() {
        return type;
    }

    Query query() {
        return query;
    }

    int history() {
        return 30;
    }

    long interval() {
        return 1;
    }

    long window() {
        return 1;
    }

    static QueryBuilder cache() {
        return queryBuilder().children().filter(context(identifier(subclassOf(Cache.class))));
    }

    static QueryBuilder childStatistic(String name) {
        return queryBuilder().children().filter(context(allOf(identifier(subclassOf(OperationStatistic.class)), attributes(hasAttribute("name", name)))));
    }
}
