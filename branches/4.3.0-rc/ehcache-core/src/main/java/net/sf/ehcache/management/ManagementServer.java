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

package net.sf.ehcache.management;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;

/**
 * Interface implemented by management servers.
 *
 * @author Ludovic Orban
 * @author brandony
 */
public interface ManagementServer {

    /**
     * Start the management server
     */
    public void start();

    /**
     * Stop the management server
     */
    public void stop();

    /**
     * Puts the submitted resource under the purview of this {@code ManagementServer}.
     *
     * @param managedResource the resource to be managed
     */
    public void register(CacheManager managedResource);

    /**
     * Removes the submitted resource under the purview of this {@code ManagementServer}.
     *
     * @param managedResource the resource to be managed
     */
    public void unregister(CacheManager managedResource);

    /**
     * Returns true if this {@code ManagementServer} has any resources registered.
     *
     * @return true if actively managing resources, false if not.
     */
    public boolean hasRegistered();

    /**
     * Initialize method to call just after instantiating the class
     *
     * @param configuration the configuration of the rest agent
     */
    public void initialize(ManagementRESTServiceConfiguration configuration);

    /**
     * Register the cluster endpoint of a specific client to enable
     * clustered monitoring.
     *
     * @param clientUUID the client UUID
     */
    public void registerClusterRemoteEndpoint(String clientUUID);

    /**
     * add a reference to the clientUUID to the EhCache Mbean
     * That allows the TSA to keep track of all the toolkit clients
     * available through an agent
     *
     * @param clientUUID the client UUID
     */
    void addClientUUID(String clientUUID);

    /**
     * remove the reference to the clientUUID from the EhCache Mbean
     *
     * @param clientUUID the client UUID
     */
    void removeClientUUID(String clientUUID);

  /**
     * Unregister the previously registered cluster endpoint.
     */
    public void unregisterClusterRemoteEndpoint();

}
