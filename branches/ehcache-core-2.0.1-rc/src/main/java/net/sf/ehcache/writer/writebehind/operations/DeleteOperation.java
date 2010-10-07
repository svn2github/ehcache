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
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the delete operation for write behind
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class DeleteOperation implements SingleOperation {
    private final CacheEntry entry;
    private final long creationTime;

    /**
     * Create a new delete operation for a particular entry
     *
     * @param entry the entry to delete
     */
    public DeleteOperation(CacheEntry entry) {
        this(entry, System.currentTimeMillis());
    }

    /**
     * Create a new delete operation for a particular entry and creation time
     *
     * @param entry        the entry to delete
     * @param creationTime the creation time of the operation
     */
    public DeleteOperation(CacheEntry entry, long creationTime) {
        this.entry = duplicateCacheEntryElement(entry);
        this.creationTime = creationTime;
    }

    private CacheEntry duplicateCacheEntryElement(CacheEntry entry) {
        if (null == entry.getElement()) {
            return entry;
        } else {
            Element element = entry.getElement();
            return new CacheEntry(entry.getKey(), new Element(element.getObjectKey(), element.getObjectValue(), element.getVersion(),
                    element.getCreationTime(), element.getLastAccessTime(), element.getHitCount(), false,
                    element.getTimeToLive(), element.getTimeToIdle(), element.getLastUpdateTime()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void performSingleOperation(CacheWriter cacheWriter) {
        cacheWriter.delete(entry);
    }

    /**
     * {@inheritDoc}
     */
    public BatchOperation createBatchOperation(List<SingleOperation> operations) {
        final List<CacheEntry> entries = new ArrayList<CacheEntry>();
        for (KeyBasedOperation operation : operations) {
            entries.add(((DeleteOperation) operation).entry);
        }
        return new DeleteAllOperation(entries);
    }

    /**
     * {@inheritDoc}
     */
    public Object getKey() {
        return entry.getKey();
    }

    /**
     * {@inheritDoc}
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Retrieves the entry that will be used for this operation
     */
    public CacheEntry getEntry() {
        return entry;
    }

    /**
     * {@inheritDoc}
     */
    public SingleOperationType getType() {
        return SingleOperationType.DELETE;
    }
}