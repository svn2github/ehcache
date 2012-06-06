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
import java.util.List;

/**
 * Allows you to explore the Terracotta cluster nodes and register for events about the cluster.
 *
 * @author Geert Bevin, Abhishek Sanoujam
 * @since 2.0
 */
public interface CacheCluster {

    /**
     * Get scheme name for this cluster info.
     *
     * @return  a scheme name for the cluster information.
     * Currently <code>TERRACOTTA</code> is the only scheme supported.
     */
    ClusterScheme getScheme();

    /**
     * Retrieves the {@code ClusterNode} instance that corresponds to the current node.
     *
     * @return the {@code ClusterNode} instance that corresponds to the current node
     */
    ClusterNode getCurrentNode();

    /**
     * Waits until the current node has successfully joined the cluster.
     *
     * @return the {@code ClusterNode} instance that corresponds to the current node
     */
    ClusterNode waitUntilNodeJoinsCluster();

    /**
     * Get all the nodes in the cluster
     *
     * @return information on all the nodes in the cluster, including ID, hostname, and IP address.
     */
    Collection<ClusterNode> getNodes();

    /**
     * Find out if the current node is connected to the cluster or not
     *
     * @return true if cluster is online otherwise false
     */
    boolean isClusterOnline();

    /**
     * Add a listener for cluster events
     *
     * @param listener Listener
     * @return True if already listening
     */
    boolean addTopologyListener(ClusterTopologyListener listener);

    /**
     * Remove a listener for cluster events
     *
     * @param listener Listener
     * @return True if not listening
     */
    boolean removeTopologyListener(ClusterTopologyListener listener);

    /**
     * Removes all listeners for cluster events
     */
    void removeAllListeners();

    /**
     * Get all the topology listeners
     *
     * @return a list of all the topology listeners
     */
    List<ClusterTopologyListener> getTopologyListeners();
}
