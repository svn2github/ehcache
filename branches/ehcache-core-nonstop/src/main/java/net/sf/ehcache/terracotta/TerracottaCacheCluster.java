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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterTopologyListener;

/**
 * {@link CacheCluster} implementation that delegates to an underlying cache cluster. The underlying {@link CacheCluster} can be changed
 * dynamically
 *
 * @author Abhishek Sanoujam
 *
 */
public class TerracottaCacheCluster implements CacheCluster {

    private final List<ClusterTopologyListener> listeners = new CopyOnWriteArrayList<ClusterTopologyListener>();
    private volatile CacheCluster realCacheCluster;

    /**
     * Set the underlying cache
     *
     * @param newCacheCluster
     */
    public void setUnderlyingCacheCluster(CacheCluster newCacheCluster) {
        this.realCacheCluster = newCacheCluster;
    }

    /**
     * Fire Rejoin event to all listeners.
     * Package protected method
     */
    void fireNodeRejoinedEvent() {
        for (ClusterTopologyListener listener : listeners) {
            listener.clusterRejoined();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean addTopologyListener(ClusterTopologyListener listener) {
        checkIfInitialized();
        boolean added = realCacheCluster.addTopologyListener(listener);
        if (added) {
            listeners.add(listener);
        }
        return added;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeTopologyListener(ClusterTopologyListener listener) {
        checkIfInitialized();
        boolean removed = realCacheCluster.removeTopologyListener(listener);
        if (removed) {
            listeners.remove(listener);
        }
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    public ClusterNode getCurrentNode() {
        checkIfInitialized();
        return realCacheCluster.getCurrentNode();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<ClusterNode> getNodes() {
        checkIfInitialized();
        return realCacheCluster.getNodes();
    }

    /**
     * {@inheritDoc}
     */
    public ClusterScheme getScheme() {
        checkIfInitialized();
        return realCacheCluster.getScheme();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterOnline() {
        checkIfInitialized();
        return realCacheCluster.isClusterOnline();
    }

    /**
     * {@inheritDoc}
     */
    public ClusterNode waitUntilNodeJoinsCluster() {
        checkIfInitialized();
        return realCacheCluster.waitUntilNodeJoinsCluster();
    }

    private void checkIfInitialized() {
        if (realCacheCluster == null) {
            throw new CacheException(
                    "The underlying cache cluster has not been initialized. Probably the terracotta client has not been configured yet.");
        }
    }

}
