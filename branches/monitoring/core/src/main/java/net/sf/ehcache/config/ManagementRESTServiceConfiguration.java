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
 * @author Ludovic Orban
 */
public class ManagementRESTServiceConfiguration {

    private volatile boolean enabled = false;
    private volatile String bind = "0.0.0.0:9889";

    /**
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @param enabled
     * @return
     */
    public ManagementRESTServiceConfiguration enabled(boolean enabled) {
        setEnabled(enabled);
        return this;
    }

    /**
     * @return
     */
    public String getBind() {
        return bind;
    }

    /**
     * @return
     */
    public String getHost() {
        if (bind == null) {
            return null;
        }
        return bind.split("\\:")[0];
    }

    /**
     * @return
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
     * @param bind
     */
    public void setBind(String bind) {
        this.bind = bind;
    }

    /**
     * @param bind
     * @return
     */
    public ManagementRESTServiceConfiguration bind(String bind) {
        setBind(bind);
        return this;
    }
}
