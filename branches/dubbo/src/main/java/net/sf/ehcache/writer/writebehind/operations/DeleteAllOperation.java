/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
package net.sf.ehcache.writer.writebehind.operations;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.writer.CacheWriter;

import java.util.List;

/**
 * Implements the delete all operation for write behind
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class DeleteAllOperation implements BatchOperation {
    private final List<CacheEntry> entries;

    /**
     * Create a new delete all operation for the provided list of cache entries
     *
     * @param entries the list of entries that are part of this operation
     */
    public DeleteAllOperation(List<CacheEntry> entries) {
        this.entries = entries;
    }

    /**
     * {@inheritDoc}
     */
    public void performBatchOperation(CacheWriter cacheWriter) {
        cacheWriter.deleteAll(entries);
    }
}
