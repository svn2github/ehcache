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
package net.sf.ehcache.cluster;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This is a dummy implementation of the CacheCluster (Null Object Pattern).  It ignores
 * all listeners and reports no nodes.
 *
 * @author Geert Bevin
 * @since 2.0
 */
public class NoopCacheCluster implements CacheCluster {

    /**
     * A singleton instance you can use rather than constructing your own.
     */
    public static final CacheCluster INSTANCE = new NoopCacheCluster();

    /**
     * {@inheritDoc}
     */
    public Collection<ClusterNode> getNodes() {
        return Collections.emptyList();
    }

    /**
     * Always returns the ClusterScheme.NONE
     *
     * @return {@link ClusterScheme#NONE}
     */
    public ClusterScheme getScheme() {
        return ClusterScheme.NONE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean addTopologyListener(ClusterTopologyListener listener) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeTopologyListener(ClusterTopologyListener listener) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterOnline() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public ClusterNode getCurrentNode() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ClusterNode waitUntilNodeJoinsCluster() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List<ClusterTopologyListener> getTopologyListeners() {
        return Collections.emptyList();
    }

    @Override
    public void removeAllListeners() {
        //
    }
}
