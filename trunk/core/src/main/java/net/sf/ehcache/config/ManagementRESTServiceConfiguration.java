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
package net.sf.ehcache.config;

/**
 *  Configuration class of management REST services.
 *
 * @author Ludovic Orban
 */
public class ManagementRESTServiceConfiguration {

    private volatile boolean enabled = false;
    private volatile String bind = "0.0.0.0:9889";

    /**
     * Check if the REST services should be enabled or not.
     * @return true if REST services should be enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set that the REST services should be enabled or disabled.
     * @param enabled true if the REST services should be enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the host:port pair to which the REST server should be bound.
     * Format is: [IP address|host name]:[port number]
     * @return the host:port pair to which the REST server should be bound.
     */
    public String getBind() {
        return bind;
    }

    /**
     * Get the host part of the host:port pair to which the REST server should be bound.
     * @return the host part of the host:port pair to which the REST server should be bound.
     */
    public String getHost() {
        if (bind == null) {
            return null;
        }
        return bind.split("\\:")[0];
    }

    /**
     * Get the port part of the host:port pair to which the REST server should be bound.
     * @return the port part of the host:port pair to which the REST server should be bound.
     */
    public int getPort() {
        if (bind == null) {
            return -1;
        }
        String[] split = bind.split("\\:");
        if (split.length != 2) {
            throw new IllegalArgumentException("invalid bind format (should be IP:port)");
        }
        return Integer.parseInt(split[1]);
    }

    /**
     * Set the host:port pair to which the REST server should be bound.
     * @param bind host:port pair to which the REST server should be bound.
     */
    public void setBind(String bind) {
        this.bind = bind;
    }
}
