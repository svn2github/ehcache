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
package net.sf.ehcache.writer.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * Implements a {@code WriterManager} that writes elements to a queue first and in the background sends the to the {@code CacheWriter}.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class WriteBehindManager implements CacheWriterManager {
    private volatile WriteBehind writeBehind;

    /**
     * {@inheritDoc}
     */
    public void init(Cache cache) throws CacheException {
        if (cache.isTerracottaClustered()) {
            writeBehind = cache.getCacheManager().createTerracottaWriteBehind(cache);
        } else {
            writeBehind = new WriteBehindQueueManager(cache.getCacheConfiguration());
        }

        CacheWriter cacheWriter = cache.getRegisteredCacheWriter();
        if (null == cacheWriter) {
            throw new CacheException("No cache writer was registered for cache " + cache.getName());
        }

        if (cache.getCacheConfiguration().getCacheWriterConfiguration().getWriteCoalescing()) {
            writeBehind.setOperationsFilter(new CoalesceKeysFilter());
        }

        writeBehind.start(cacheWriter);
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element) throws CacheException {
        writeBehind.write(element);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(CacheEntry entry) throws CacheException {
        writeBehind.delete(entry);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        /* If WriteBehind is configured but no writer is registered, writeBehind will be null */
        if (writeBehind != null) {
            writeBehind.stop();
        }
    }

    /**
     * Gets the best estimate for items in the queue still awaiting processing.
     * Not including elements currently processed
     * @return the amount of elements still awaiting processing.
     */
    public long getQueueSize() {
        return writeBehind.getQueueSize();
    }
}
