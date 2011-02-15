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

import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.store.TerracottaStore;

/**
 * Interface for nonstop feature
 *
 * @author Abhishek Sanoujam
 *
 */
public interface NonstopStore extends TerracottaStore, CacheLockProvider {

    /**
     * Execute the {@link ClusterOperation} within this {@link NonstopStore} context.
     *
     * @param <V> Return type depending on the {@link ClusterOperation}
     * @param operation
     * @return the return value depending on the {@link ClusterOperation}
     */
    public <V> V executeClusterOperation(ClusterOperation<V> operation);
}
