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

package net.sf.ehcache.terracotta;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.writer.writebehind.WriteBehind;

/**
 * A {@link ClusteredInstanceFactory} implementation that delegates all operations to an underlying delegate except for the following
 * operations:
 *<ul>
 *<li>{@link #getTopology()} : Delegates to the {@link TerracottaClient#getCacheCluster()}</li>
 *</ul>
 *
 * @author Abhishek Sanoujam
 *
 */
public class ClusteredInstanceFactoryWrapper implements ClusteredInstanceFactory {

    private final TerracottaClient client;
    private final ClusteredInstanceFactory delegate;

    /**
     * Constructor accepting the TerracottaClient and the actual factory
     *
     * @param client
     * @param delegate
     */
    public ClusteredInstanceFactoryWrapper(TerracottaClient client, ClusteredInstanceFactory delegate) {
        this.client = client;
        this.delegate = delegate;

    }

    /**
     * Returns the actual underlying factory
     * @return the actual underlying factory
     */
    protected ClusteredInstanceFactory getActualFactory() {
        return delegate;
    }

    /**
     * {@inheritDoc}
     */
    public CacheCluster getTopology() {
        client.waitUntilRejoinComplete();
        return client.getCacheCluster();
    }

    // all methods below delegate to the real factory

    /**
     * {@inheritDoc}
     */
    public String getUUID() {
        client.waitUntilRejoinComplete();
        return delegate.getUUID();
    }

    /**
     * {@inheritDoc}
     */
    public CacheEventListener createEventReplicator(Ehcache cache) {
        client.waitUntilRejoinComplete();
        return delegate.createEventReplicator(cache);
    }

    /**
     * {@inheritDoc}
     */
    public Store createStore(Ehcache cache) {
        client.waitUntilRejoinComplete();
        return delegate.createStore(cache);
    }

    /**
     * {@inheritDoc}
     */
    public WriteBehind createWriteBehind(Ehcache cache) {
        client.waitUntilRejoinComplete();
        return delegate.createWriteBehind(cache);
    }

    /**
     * {@inheritDoc}
     */
    public SoftLockFactory getOrCreateSoftLockFactory(Ehcache cache) {
        client.waitUntilRejoinComplete();
        return delegate.getOrCreateSoftLockFactory(cache);
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        delegate.shutdown();
    }

}
