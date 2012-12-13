/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import net.sf.ehcache.Cache;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.writebehind.WriteBehindQueueManager;
import org.terracotta.context.query.Query;
import org.terracotta.context.query.QueryBuilder;
import org.terracotta.statistics.ValueStatistic;

import static org.terracotta.context.query.Matchers.*;
import static org.terracotta.context.query.QueryBuilder.*;

/**
 *
 * @author cdennis
 */
public enum PassThroughType {

    CACHE_SIZE(Integer.TYPE, null, cache().chain(childStatistic("cache-size")).ensureUnique().build()), 
    LOCAL_HEAP_SIZE(Integer.TYPE, 0, stores().chain(childStatistic("local-heap-size")).build()),
    LOCAL_HEAP_SIZE_BYTES(Long.TYPE, 0L, stores().chain(childStatistic("local-heap-size-in-bytes")).build()),
    LOCAL_OFFHEAP_SIZE(Long.TYPE, 0L, stores().chain(childStatistic("local-offheap-size")).build()),
    LOCAL_OFFHEAP_SIZE_BYTES(Long.TYPE, 0L, stores().chain(childStatistic("local-offheap-size-in-bytes")).build()), 
    LOCAL_DISK_SIZE(Long.TYPE, 0L, stores().chain(childStatistic("local-disk-size")).build()),
    LOCAL_DISK_SIZE_BYTES(Long.TYPE, 0L, stores().chain(childStatistic("local-disk-size-in-bytes")).build()), 
    WRITER_QUEUE_LENGTH(Long.TYPE, 0L, queryBuilder().descendants().filter(context(identifier(subclassOf(WriteBehindQueueManager.class)))).chain(childStatistic("write-behind-queue-length")).build());
    
    private final Class<?> type;
    private final Object absentValue;
    private final Query query;

    private <T> PassThroughType(Class<T> type, T absentValue, Query query) {
        this.type = type;
        this.absentValue = absentValue;
        this.query = query;
    }

    final Class<?> type() {
        return type;
    }

    final Object absentValue() {
        return absentValue;
    }
    
    final Query query() {
        return query;
    }

    static QueryBuilder cache() {
        return queryBuilder().children().filter(context(identifier(subclassOf(Cache.class))));
    }

    static QueryBuilder stores() {
        return queryBuilder().descendants().filter(context(identifier(subclassOf(Store.class))));
    }
    
    static Query childStatistic(String name) {
        return queryBuilder().children().filter(context(allOf(identifier(subclassOf(ValueStatistic.class)), attributes(hasAttribute("name", name))))).build();
    }
}
