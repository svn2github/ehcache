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
import net.sf.ehcache.Ehcache;
import org.terracotta.context.query.Query;

import static net.sf.ehcache.statistics.extended.EhcacheQueryBuilder.cache;
import static net.sf.ehcache.statistics.extended.EhcacheQueryBuilder.descendants;

/**
 *
 * @author cdennis
 */
public enum StandardPassThroughStatistic {

    /** cache size */
    CACHE_SIZE(cache(), Integer.TYPE, null, "size", "cache"),

    /** local heap size in entries */
    LOCAL_HEAP_SIZE(cache().children().exclude(Ehcache.class).add(descendants()), Integer.TYPE, 0, "size", "local-heap"),

    /** local hea size in bytes*/
    LOCAL_HEAP_SIZE_BYTES(cache().children().exclude(Ehcache.class).add(descendants()), Long.TYPE, 0L, "size-in-bytes", "local-heap"),

    /** local off heap size in entry count */
    LOCAL_OFFHEAP_SIZE(cache().children().exclude(Ehcache.class).add(descendants()), Long.TYPE, 0L, "size", "local-offheap"),

    /** local offheap size in bytes */
    LOCAL_OFFHEAP_SIZE_BYTES(cache().children().exclude(Ehcache.class).add(descendants()), Long.TYPE, 0L, "size-in-bytes", "local-offheap"),

    /** local disk size in entries */
    LOCAL_DISK_SIZE(cache().children().exclude(Ehcache.class).add(descendants()), Integer.TYPE, 0, "size", "local-disk"),

    /** local disk size in bytes */
    LOCAL_DISK_SIZE_BYTES(cache().children().exclude(Ehcache.class).add(descendants()), Long.TYPE, 0L, "size-in-bytes", "local-disk"),

    /** writer queue length */
    WRITER_QUEUE_LENGTH(cache().descendants(), Long.TYPE, 0L, "queue-length", "write-behind"),

    /** remote size */
    REMOTE_SIZE(cache().descendants(), Long.TYPE, 0L, "size", "remote"),
    
    /** last rejoin time */
    LAST_REJOIN_TIMESTAMP(cache().descendants(), Long.TYPE, 0L, "lastRejoinTime", "cache");

    private static final int THIRTY = 30;
    
    private final Class<? extends Number> type;
    private final Query context;
    private final Number absentValue;
    private final String name;
    private final Set<String> tags;

    private <T extends Number> StandardPassThroughStatistic(Query context, Class<T> type, T absentValue, String name, String ... tags) {
        this.context = context;
        this.type = type;
        this.absentValue = absentValue;
        this.name = name;
        this.tags = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(tags)));
    }
    
    /**
     * Query that select context nodes for this statistic.
     * 
     * @return context query
     */
    final Query context() {
        return context;
    }
    
    /**
     * Statistic value type.
     *
     * @return value type
     */
    public final Class<? extends Number> type() {
        return type;
    }

    /**
     * Statistic value when absent.
     *
     * @return absent value
     */
    public final Number absentValue() {
        return absentValue;
    }

    /**
     * The name of the statistic as found in the statistics context tree.
     *
     * @return the statistic name
     */
    public final String statisticName() {
        return name;
    }

    /**
     * A set of tags that will be on the statistic found in the statistics context tree.
     *
     * @return the statistic tags
     */
    public final Set<String> tags() {
        return tags;
    }

}
