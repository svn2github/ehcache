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

/**
 * Represents a config error in a cache configuration
 * @author Alex Snaps
 */
public class CacheConfigError extends ConfigError {
    private final String cacheName;

    /**
     * Constructor
     * @param error the error message
     * @param cacheName the cache name for which this error occured
     */
    public CacheConfigError(final String error, final String cacheName) {
        super(error);
        this.cacheName = cacheName;
    }

    /**
     * Returns the cache name
     * @return cache name
     */
    public String getCacheName() {
        return cacheName;
    }

    @Override
    public String toString() {
        return "Cache '" + cacheName + "' error: " + getError();
    }
}
