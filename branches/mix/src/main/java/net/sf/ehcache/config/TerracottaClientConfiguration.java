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

package net.sf.ehcache.config;

import net.sf.ehcache.CacheException;

/**
 * Holds the Terracotta configuration for a particular client
 *
 * @author amiller@terracotta.org
 * @author Abhishek Sanoujam
 */
public class TerracottaClientConfiguration implements Cloneable {

    /**
     * Default value of rejoin attribute
     */
    public static final boolean DEFAULT_REJOIN_VALUE = false;

    private static final String TC_CONFIG_HEADER = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">";
    private static final String TC_CONFIG_FOOTER = "</tc:tc-config>";

    private String url;
    private String embeddedConfig;
    private boolean rejoin = DEFAULT_REJOIN_VALUE;
    private volatile boolean configFrozen;

    /**
     * Clones this object, following the usual contract.
     *
     * @return a copy, which independent other than configurations than cannot change.
     * @throws CloneNotSupportedException
     */
    @Override
    public TerracottaClientConfiguration clone() throws CloneNotSupportedException {
        return (TerracottaClientConfiguration) super.clone();
    }

    /**
     * Builder method to set the URL.
     *
     * @param url
     *            the URL to set
     * @return this configuration instance
     */
    public final TerracottaClientConfiguration url(String url) {
        setUrl(url);
        return this;
    }

    /**
     * Builder method to set the URL for a host and a port.
     *
     * @param host
     *            the host where to get the Terracotta configuration from
     * @param port
     *            the port on that host
     * @return this configuration instance
     */
    public final TerracottaClientConfiguration url(String host, String port) {
        setUrl(host + ":" + port);
        return this;
    }

    /**
     * Set url
     */
    public final void setUrl(String url) {
        this.url = url;
        validateConfiguration();
    }

    /**
     * Get url string
     */
    public final String getUrl() {
        return this.url;
    }

    /**
     * Tell the BeanHandler to extract the entire subtree xml as text at element <tc-config/>. Expects
     * to receive the contents of the <tc-config/> tag and will wrap it in a proper tc-config header / footer.
     */
    public final void extractTcconfig(String text) {
        this.embeddedConfig = text;
        validateConfiguration();
    }

    /**
     * Get the embedded config read as <tc-config/>
     */
    public final String getEmbeddedConfig() {
        return TC_CONFIG_HEADER + embeddedConfig + TC_CONFIG_FOOTER;
    }

    /**
     * Get the original embedded config
     *
     * @return original embedded config
     */
    public final String getOriginalEmbeddedConfig() {
        return embeddedConfig;
    }

    /**
     * Helper to check whether this is url config or embedded config
     */
    public final boolean isUrlConfig() {
        return this.url != null;
    }

    private void validateConfiguration() {
        if (this.url != null && this.embeddedConfig != null) {
            throw new InvalidConfigurationException("It is invalid to specify both a config url and "
                    + "an embedded config in the <terracottaConfig> element.");
        }
    }

    /**
     * Returns true if rejoin is enabled
     *
     * @return the rejoin
     */
    public boolean isRejoin() {
        return rejoin;
    }

    /**
     * Set rejoin value
     *
     * @param rejoin the rejoin to set
     */
    public void setRejoin(boolean rejoin) {
        if (configFrozen) {
            throw new CacheException("Cannot enable/disable rejoin once config has been frozen");
        }
        this.rejoin = rejoin;
    }

    /**
     * Builder method to set rejoin
     *
     * @param rejoin
     * @return this instance
     */
    public TerracottaClientConfiguration rejoin(boolean rejoin) {
        this.setRejoin(rejoin);
        return this;
    }

    /**
     * Freezes the config
     */
    public void freezeConfig() {
        configFrozen = true;
    }

}
