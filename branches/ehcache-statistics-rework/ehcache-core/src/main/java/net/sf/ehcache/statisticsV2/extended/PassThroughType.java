/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import net.sf.ehcache.Cache;
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

    CACHE_SIZE(Integer.TYPE, null, cache().chain(childStatistic("cache-size").build()).ensureUnique().build()), 
    LOCAL_HEAP_SIZE(Long.TYPE, 0L, queryBuilder().empty().build()),
    LOCAL_HEAP_SIZE_BYTES(Long.TYPE, 0L, queryBuilder().empty().build()),
    LOCAL_OFFHEAP_SIZE(Long.TYPE, 0L, queryBuilder().empty().build()),
    LOCAL_OFFHEAP_SIZE_BYTES(Long.TYPE, 0L, queryBuilder().empty().build()), 
    LOCAL_DISK_SIZE(Long.TYPE, 0L, queryBuilder().empty().build()),
    LOCAL_DISK_SIZE_BYTES(Long.TYPE, 0L, queryBuilder().empty().build()), 
    WRITER_QUEUE_SIZE(Long.TYPE, 0L, queryBuilder().empty().build()),
    WRITER_QUEUE_LENGTH(Long.TYPE, 0L, queryBuilder().empty().build());
    
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

    static QueryBuilder childStatistic(String name) {
        return queryBuilder().children().filter(context(allOf(identifier(subclassOf(ValueStatistic.class)), attributes(hasAttribute("name", name)))));
    }
}
