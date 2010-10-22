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
package net.sf.ehcache.writer.writebehind.operations;

import net.sf.ehcache.writer.CacheWriter;

import java.util.List;

/**
 * Interface to implement single operations that are performed with write-behind
 *
 * @author Geert Bevin
 * @version $Id$
 */
public interface SingleOperation extends KeyBasedOperation {
    /**
     * Perform this operation as a single execution with the provided cache writer
     *
     * @param cacheWriter the cache writer this operation should be performed upon
     */
    public void performSingleOperation(CacheWriter cacheWriter);

    /**
     * Creates a batch operation that corresponds to the operation type of this single operation.
     * <p/>
     * This batch operation will not be stored in the queue anymore and is solely used for structuring.
     * The data from the single operation will already be processed in the final form that will be expected by the
     * {@code CacheWriter} that will be used to execute the batch operation.
     *
     * @param operations the single operations that need to be regrouped in the batch operation
     * @return the created batch operation
     */
    public BatchOperation createBatchOperation(List<SingleOperation> operations);

    /**
     * Returns a stable identifier for the type this operation can be classified in. This is used to group and order
     * batched operations.
     *
     * @return the identifier for this operation type
     */
    public SingleOperationType getType();
}
