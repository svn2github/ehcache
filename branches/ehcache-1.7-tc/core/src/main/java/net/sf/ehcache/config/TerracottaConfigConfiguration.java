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
 * @author amiller@terracotta.org
 */
public class TerracottaConfigConfiguration implements Cloneable {

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
    }

    /**
     * Get url string
     */
    final public String getUrl() {
        return this.url;
    }
    
    /**
     * Tell the BeanHandler to extract the entire subtree xml as text at element <tc-config/>
     */
    final public void extractTcconfig(String text) {
        this.embeddedConfig = text;
    }

    /**
     * Get the embedded config read as <tc-config/>
     */
    final public String getEmbeddedConfig() {
        return this.embeddedConfig;
    }

    /**
     * Helper to check whether this is url config or embedded config
     */
    final public boolean isUrlConfig() {
        return this.url != null;
    }
    
}
