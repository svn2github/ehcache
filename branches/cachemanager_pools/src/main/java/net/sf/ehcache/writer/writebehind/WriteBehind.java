/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

/**
 * An interface for write behind behavior.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public interface WriteBehind {
    /**
     * Start the write behind queue with a cache writer
     *
     * @param writer the cache writer that should be used to process the operations
     * @see #stop
     */
    void start(CacheWriter writer) throws CacheException;

    /**
     * Add a write operation for a given element.
     *
     * @param element the element for which a write operation will be added to the write behind queue
     */
    void write(Element element);

    /**
     * Add a delete operation for the given cache entry
     *
     * @param entry the cache entry for which a delete operation will be added to the write behind queue
     */
    void delete(CacheEntry entry);

    /**
     * Set the operations filter that should be used.
     *
     * @param filter the filter that will be used as of now
     */
    void setOperationsFilter(OperationsFilter filter);

    /**
     * Stop the coordinator and all the internal data structures.
     * <p/>
     * This stops as quickly as possible without losing any previously added items. However, no guarantees are made
     * towards the processing of these items. It's highly likely that items are still inside the internal data structures
     * and not processed.
     *
     * @see #start
     */
    void stop() throws CacheException;

    /**
     * Gets the best estimate for items in the queue still awaiting processing.
     * Not including elements currently processed
     * @return the amount of elements still awaiting processing.
     */
    long getQueueSize();
}
