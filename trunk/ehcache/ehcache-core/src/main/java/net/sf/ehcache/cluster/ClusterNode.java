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

/**
 * Provides information about a node in a cache cluster.
 *
 * @author Geert Bevin
 * @since 2.0
 */
public interface ClusterNode {

    /**
     * Get a unique (per cluster) identifier for this node.
     *
     * @return Unique per cluster identifier
     */
    String getId();

    /**
     * Get the host name of the node
     *
     * @return Host name of node
     */
    String getHostname();

    /**
     * Get the IP address of the node
     *
     * @return IP address of node
     */
    String getIp();

}
