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
package net.sf.ehcache.management.event;

import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaClient;

import java.io.Serializable;

/**
 * @author Ludovic Orban
 */
public class DelegatingManagementEventSink implements ManagementEventSink {

    private final TerracottaClient terracottaClient;
    private volatile ClusteredInstanceFactory clusteredInstanceFactory;
    private volatile ManagementEventSink managementEventSink;

    /**
     * @param terracottaClient a terracotta client
     */
    public DelegatingManagementEventSink(TerracottaClient terracottaClient) {
        this.terracottaClient = terracottaClient;
    }

    private ManagementEventSink get() {
        ClusteredInstanceFactory cif = terracottaClient.getClusteredInstanceFactory();
        if (cif != null && cif != this.clusteredInstanceFactory) {
            this.managementEventSink = cif.createEventSink();
            this.clusteredInstanceFactory = cif;
        }
        return managementEventSink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendManagementEvent(Serializable event, String type) {
        ManagementEventSink sink = get();
        if (sink != null) {
            sink.sendManagementEvent(event, type);
        }
    }
}
