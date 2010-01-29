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
    private final Object key;
    private final long creationTime;

    /**
     * Create a new delete operation for a particular key
     *
     * @param key the key to delete
     */
    public DeleteOperation(Object key) {
        this(key, System.currentTimeMillis());
    }

    /**
     * Create a new delete operation for a particular key and creation time
     *
     * @param key          the key to delete
     * @param creationTime the creation time of the operation
     */
    public DeleteOperation(Object key, long creationTime) {
        this.key = key;
        this.creationTime = creationTime;
    }

    /**
     * {@inheritDoc}
     */
    public void performSingleOperation(CacheWriter cacheWriter) {
        cacheWriter.delete(key);
    }

    /**
     * {@inheritDoc}
     */
    public BatchOperation createBatchOperation(List<SingleOperation> operations) {
        final List<Object> keys = new ArrayList<Object>();
        for (KeyBasedOperation operation : operations) {
            keys.add(((DeleteOperation) operation).key);
        }
        return new DeleteAllOperation(keys);
    }

    /**
     * {@inheritDoc}
     */
    public Object getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    public long getCreationTime() {
        return creationTime;
    }
}