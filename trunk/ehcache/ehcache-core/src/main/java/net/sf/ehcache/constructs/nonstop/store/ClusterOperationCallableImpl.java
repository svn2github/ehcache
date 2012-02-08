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

package net.sf.ehcache.constructs.nonstop.store;

import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.ClusterOperationCallable;

/**
 * Implementation of {@link ClusterOperationCallable}
 *
 * @author Abhishek Sanoujam
 *
 * @param <V>
 */
class ClusterOperationCallableImpl<V> implements ClusterOperationCallable<V> {

    private final ClusterOperation<V> clusterOperation;

    /**
     * Public constructor
     *
     * @param clusterOperation
     */
    public ClusterOperationCallableImpl(ClusterOperation<V> clusterOperation) {
        this.clusterOperation = clusterOperation;
    }

    /**
     * {@inheritDoc}
     */
    public V call() throws Exception {
        return clusterOperation.performClusterOperation();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.constructs.nonstop.store.ClusterOperationCallableI#getClusterOperation()
     */
    public ClusterOperation<V> getClusterOperation() {
        return clusterOperation;
    }

}
