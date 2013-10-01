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

package net.sf.ehcache;

import static net.sf.ehcache.statistics.StatisticBuilder.operation;
import net.sf.ehcache.CacheOperationOutcomes.ClusterEventOutcomes;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;

/**
 * A listener to capture statistics relating to cluster events regarding
 * this node. This should really be a single listener, at the cache manager level,
 * with CacheManager statistics. But we don't have that concept at the moment.
 * Sad face. 
 * 
 * @author cschanck
 * 
 */
public class CacheClusterStateStatisticsListener implements ClusterTopologyListener {

    /** The cluster observer. */
    private final OperationObserver<ClusterEventOutcomes> clusterObserver = operation(ClusterEventOutcomes.class).named("cluster").of(this)
            .tag("cache").build();

    /** The last rejoin time stamp. */
    private volatile long mostRecentRejoinTimeStamp = 0L;

    /** The cache. */
    private final Cache cache;

    /**
     * Instantiates a new cluster state statistics listener.
     * 
     * @param cache the cache
     */
    CacheClusterStateStatisticsListener(Cache cache) {
        this.cache = cache;
        StatisticsManager.associate(this).withParent(cache);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.cluster.ClusterTopologyListener#nodeLeft(net.sf.ehcache.cluster.ClusterNode)
     */
    @Override
    public void nodeLeft(ClusterNode node) {
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.cluster.ClusterTopologyListener#nodeJoined(net.sf.ehcache.cluster.ClusterNode)
     */
    @Override
    public void nodeJoined(ClusterNode node) {
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.cluster.ClusterTopologyListener#clusterRejoined(net.sf.ehcache.cluster.ClusterNode,
     * net.sf.ehcache.cluster.ClusterNode)
     */
    @Override
    public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
        if (newNode.equals(this.cache.getCacheManager().getCluster(ClusterScheme.TERRACOTTA).getCurrentNode())) {
            mostRecentRejoinTimeStamp = System.currentTimeMillis();
            clusterObserver.end(ClusterEventOutcomes.REJOINED);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.cluster.ClusterTopologyListener#clusterOnline(net.sf.ehcache.cluster.ClusterNode)
     */
    @Override
    public void clusterOnline(ClusterNode node) {
        if (node.equals(this.cache.getCacheManager().getCluster(ClusterScheme.TERRACOTTA).getCurrentNode())) {
            clusterObserver.end(ClusterEventOutcomes.ONLINE);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.cluster.ClusterTopologyListener#clusterOffline(net.sf.ehcache.cluster.ClusterNode)
     */
    @Override
    public void clusterOffline(ClusterNode node) {
        if (node.equals(this.cache.getCacheManager().getCluster(ClusterScheme.TERRACOTTA).getCurrentNode())) {
            clusterObserver.end(ClusterEventOutcomes.OFFLINE);
        }
    }

    /**
     * Gets the last rejoin time stamp nanos.
     * 
     * @return the last rejoin time stamp nanos
     */
    @org.terracotta.statistics.Statistic(name = "lastRejoinTime", tags = "cache")
    public long getMostRecentRejoinTimeStampMS() {
        return mostRecentRejoinTimeStamp;
    }

}