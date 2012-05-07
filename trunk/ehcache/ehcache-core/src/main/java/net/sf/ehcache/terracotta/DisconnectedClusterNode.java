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

import net.sf.ehcache.cluster.ClusterNode;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ClusterNode which copies a disconnected ClusterNode without keeping any reference to the original one.
 *
 * @author Ludovic Orban
 */
public class DisconnectedClusterNode implements ClusterNode {

  private final String id;

  /**
   * Constructor accepting the disconnected node
   *
   * @param node the node to copy
   */
  public DisconnectedClusterNode(final ClusterNode node) {
    this.id = node.getId();
  }

  /**
   * {@inheritDoc}
   */
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  public String getHostname() {
    String hostName;
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostName = "[Can't determine hostname and " + id + " has DISCONNECTED]";
    }
    return hostName;
  }

  /**
   * {@inheritDoc}
   */
  public String getIp() {
    String ip;
    try {
      ip = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      ip = "[Can't determine IP and " + id + " has DISCONNECTED]";
    }
    return ip;
  }

}
