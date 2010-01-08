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

package net.sf.ehcache.config;

/**
 * Holds the Terracotta clustered configuration
 * 
 * @author amiller@terracotta.org
 */
public class TerracottaConfigConfiguration implements Cloneable {
    private static final String TC_CONFIG_HEADER = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">";
    private static final String TC_CONFIG_FOOTER = "</tc:tc-config>";

    private String url;
    private String embeddedConfig;

    /**
     * Clones this object, following the usual contract.
     * 
     * @return a copy, which independent other than configurations than cannot change.
     * @throws CloneNotSupportedException
     */
    @Override
    public TerracottaConfigConfiguration clone() throws CloneNotSupportedException {
        return (TerracottaConfigConfiguration) super.clone();
    }

    /**
     * Set url
     */
    final public void setUrl(String url) {
        this.url = url;
        validateConfiguration();
    }

    /**
     * Get url string
     */
    final public String getUrl() {
        return this.url;
    }

    /**
     * Tell the BeanHandler to extract the entire subtree xml as text at element <tc-config/>. Expects
     * to receive the contents of the <tc-config/> tag and will wrap it in a proper tc-config header / footer.
     */
    final public void extractTcconfig(String text) {
        this.embeddedConfig = text;
        validateConfiguration();
    }

    /**
     * Get the embedded config read as <tc-config/>
     */
    final public String getEmbeddedConfig() {
        return TC_CONFIG_HEADER + embeddedConfig + TC_CONFIG_FOOTER;
    }

    /**
     * Get the original embedded config
     * 
     * @return original embedded config
     */
    final public String getOriginalEmbeddedConfig() {
        return embeddedConfig;
    }

    /**
     * Helper to check whether this is url config or embedded config
     */
    final public boolean isUrlConfig() {
        return this.url != null;
    }

    private void validateConfiguration() {
        if (this.url != null && this.embeddedConfig != null) {
            throw new InvalidConfigurationException("It is invalid to specify both a config url and "
                    + "an embedded config in the <terracottaConfig> element.");
        }
    }

}
