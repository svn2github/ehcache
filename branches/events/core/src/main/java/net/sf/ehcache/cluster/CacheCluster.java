/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

/**
 * Allows you to explore the Terracotta cluster nodes and register for events about the cluster.
 * @author Alex Miller
 */
public interface CacheCluster {

    /**
     * Get scheme name for this cluster info
     * @return Scheme name
     */
    String getScheme();

    /**
     * Get all the nodes in the cluster
     * @return All the ClusterNodes 
     */
    Collection<ClusterNode> getNodes();
    
    /**
     * Add a listener for cluster events
     * @param listener Listener
     * @return True if already listening
     */
    boolean addTopologyListener(ClusterTopologyListener listener);
    
    /**
     * Remove a listener for cluster events
     * @param listener Listener
     * @return True if not listening
     */
    boolean removeTopologyListener(ClusterTopologyListener listener);
}
