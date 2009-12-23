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

package net.sf.ehcache.server.jaxb;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Greg Luck
 */
@XmlRootElement
public class Cache {
    private String name;

    private String uri;

    private String description;

    private Statistics statistics;
    private CacheConfiguration cacheConfiguration;


    /**
     * Empty Constructor
     */
    public Cache() {
    }

    /**
     * Constructs a Cache Representation from a core Ehcache
     *
     * @param ehcache
     */
    public Cache(Ehcache ehcache) {
        this.name = ehcache.getName();
        this.uri = "rest/" + name;
        this.description = ehcache.toString();

    }

    /**
     * Full Constructor
     */
    public Cache(String name, String uri, String description,
                 Statistics statistics, CacheConfiguration cacheConfiguration) {
        setName(name);
        setUri(uri);
        setDescription(description);
        this.statistics = statistics;
        this.cacheConfiguration = cacheConfiguration;
    }


    /**
     * @return The cache name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the cache name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The full URI of the resource
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return the current statistics for this cache
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * @param uri the full URI of the resource
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Gets the description of the cache.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the cache.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the statistics
     *
     * @param statistics a JAXB statistics object
     */
    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    /**
     * @return the active configuration of this cache
     */
    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    /**
     * @param cacheConfiguration the active configuration of this cache
     */
    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    /**
     * Clones the resource using a deep copy.
     */
    public Cache clone() throws CloneNotSupportedException {
        Cache cache = (Cache) super.clone();
        cache.setName(name);
        cache.setUri(uri);
        return cache;
    }

}