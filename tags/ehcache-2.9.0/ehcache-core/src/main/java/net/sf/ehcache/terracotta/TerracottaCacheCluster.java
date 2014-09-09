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

package net.sf.ehcache.terracotta;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CacheCluster} implementation that delegates to an underlying cache cluster. The underlying {@link CacheCluster} can be changed
 * dynamically
 *
 * @author Abhishek Sanoujam
 *
 */
public class TerracottaCacheCluster implements CacheCluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerracottaCacheCluster.class);
    private final List<ClusterTopologyListener> listeners = new CopyOnWriteArrayList<ClusterTopologyListener>();
    private volatile CacheCluster realCacheCluster;

    /**
     * Set the underlying cache
     *
     * @param newCacheCluster
     */
    public void setUnderlyingCacheCluster(CacheCluster newCacheCluster) {
        if (newCacheCluster == null) {
            throw new IllegalArgumentException("CacheCluster can't be null");
        }
        final CacheCluster oldRealCacheCluster = this.realCacheCluster;
        this.realCacheCluster = newCacheCluster;
        for (ClusterTopologyListener listener : listeners) {
            this.realCacheCluster.addTopologyListener(listener);
        }
        if (oldRealCacheCluster != null) {
            for (ClusterTopologyListener listener : listeners) {
                oldRealCacheCluster.removeTopologyListener(listener);
            }
        }
    }

    /**
     * Fire Rejoin event to all listeners.
     * Package protected method
     *
     * @param oldNode
     * @param newNode
     */
    void fireNodeRejoinedEvent(ClusterNode oldNode, ClusterNode newNode) {
        Set<ClusterTopologyListener> firedToListeners = new HashSet<ClusterTopologyListener>();
        for (ClusterTopologyListener listener : realCacheCluster.getTopologyListeners()) {
            firedToListeners.add(listener);
            fireRejoinEvent(oldNode, newNode, listener);
        }
        for (ClusterTopologyListener listener : listeners) {
            if (firedToListeners.contains(listener)) {
                continue;
            }
            fireRejoinEvent(oldNode, newNode, listener);
        }
    }

    private void fireRejoinEvent(ClusterNode oldNode, ClusterNode newNode, ClusterTopologyListener listener) {
        try {
            listener.clusterRejoined(oldNode, newNode);
        } catch (Throwable e) {
            LOGGER.error("Caught exception while firing rejoin event", e);
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

    /**
     * {@inheritDoc}
     */
    public List<ClusterTopologyListener> getTopologyListeners() {
        return this.listeners;
    }

    @Override
    public void removeAllListeners() {
        checkIfInitialized();
        realCacheCluster.removeAllListeners();
        listeners.clear();

    }

}
