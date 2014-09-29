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

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.writer.writebehind.operations.SingleOperation;

/**
 * An implementation of write behind with a queue that is kept in non durable local heap.
 *
 * @author Geert Bevin
 * @version $Id$
 */
class WriteBehindQueue extends AbstractWriteBehindQueue {

    private List<SingleOperation> waiting = new ArrayList<SingleOperation>();

    /**
     * Construct a simple list backed write behind queue.
     *
     * @param config
     */
    WriteBehindQueue(CacheConfiguration config) {
        super(config);
    }

    @Override
    protected List<SingleOperation> quarantineItems() {
        List<SingleOperation> quarantined = waiting;
        waiting = new ArrayList<SingleOperation>();
        return quarantined;
    }

    @Override
    protected void addItem(SingleOperation operation) {
        waiting.add(operation);
    }

    @Override
    public long getQueueSize() {
        return waiting.size();
    }

    @Override
    protected void reinsertUnprocessedItems(List<SingleOperation> operations) {
        List<SingleOperation> newQueue = new ArrayList<SingleOperation>(operations);
        newQueue.addAll(waiting);
        waiting = newQueue;
    }

}
