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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterTopologyListener;

public class MockCacheCluster implements CacheCluster {

    private final List<ClusterTopologyListener> listeners = new CopyOnWriteArrayList<ClusterTopologyListener>();
    private final ClusterNode currentNode = new ClusterNode() {

        public String getIp() {
            return "127.0.0.1";
        }

        public String getId() {
            return "1";
        }

        public String getHostname() {
            return "dummyHostName";
        }
    };

    public void fireCurrentNodeLeft() {
        for (ClusterTopologyListener listener : listeners) {
            listener.nodeLeft(currentNode);
        }
    }

    public void fireClusterOffline() {
        for (ClusterTopologyListener listener : listeners) {
            listener.clusterOffline(currentNode);
        }
    }

    public void fireClusterOnline() {
        for (ClusterTopologyListener listener : listeners) {
            listener.clusterOnline(currentNode);
        }
    }

    public boolean addTopologyListener(ClusterTopologyListener listener) {
        return listeners.add(listener);
    }

    public ClusterNode getCurrentNode() {
        return currentNode;
    }

    public Collection<ClusterNode> getNodes() {
        return Collections.singletonList(currentNode);
    }

    public ClusterScheme getScheme() {
        return ClusterScheme.TERRACOTTA;
    }

    public boolean isClusterOnline() {
        return true;
    }

    public boolean removeTopologyListener(ClusterTopologyListener listener) {
        return listeners.remove(listener);
    }

    public ClusterNode waitUntilNodeJoinsCluster() {
        return currentNode;
    }

    public List<ClusterTopologyListener> getTopologyListeners() {
        return this.listeners;
    }

}
