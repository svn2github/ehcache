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
    WRITER_QUEUE_LENGTH(Long.TYPE, 0L, queryBuilder().descendants().filter(context(identifier(subclassOf(WriteBehindQueueManager.class)))).chain(childStatistic("write-behind-queue-length")).build()),
    REMOTE_SIZE(Long.TYPE, 0L, stores().chain(childStatistic("remote-size")).build());

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
