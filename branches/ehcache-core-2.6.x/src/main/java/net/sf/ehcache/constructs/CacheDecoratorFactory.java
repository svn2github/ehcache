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

package net.sf.ehcache.constructs;

import java.util.Properties;

import net.sf.ehcache.Ehcache;

/**
 * An abstract factory for creating decorated Ehcache instances. Implementing classes should provide their own
 * concrete factory extending this factory.
 * 
 * @author Abhishek Sanoujam
 */
public abstract class CacheDecoratorFactory {

    /**
     * Dash string : "-"
     */
    public static final String DASH = "-";

    /**
     * Creates a decorated {@link Ehcache} using the properties specified for configuring the decorator.
     * <p />
     * If the returned decorated cache has the same name as the underlying cache, then the original cache will be replaced by this new
     * decorated cache in the CacheManager.
     * 
     * @param cache
     *            a reference to the owning cache
     * @param properties
     *            implementation specific properties configured as delimiter
     *            separated name value pairs in ehcache.xml
     * @return a decorated Ehcache
     */
    public abstract Ehcache createDecoratedEhcache(Ehcache cache, Properties properties);

    /**
     * This method is called when the factory is specified for the defaultCache in the config.
     * Create the decorated {@link Ehcache} using the properties specified.
     * <p />
     * If the returned decorated cache has the same name as the underlying cache, then the original cache will be replaced by this new
     * decorated cache in the CacheManager.
     * 
     * @param cache
     *            a reference to the owning cache
     * @param properties
     *            implementation specific properties configured as delimiter
     *            separated name value pairs in ehcache.xml
     * @return a decorated Ehcache
     */
    public abstract Ehcache createDefaultDecoratedEhcache(Ehcache cache, Properties properties);

    /**
     * Utility method to generate name of decorated cache to be created using factory specified in defaultCache.
     * 
     * @param cache
     *            the underlying cache
     * @param cacheNameSuffix
     *            Name to be used as suffix. This is normally provided as a property in the decorator config properties. If this parameter
     *            is null or empty string, cache.getName() is returned
     * @return Name to be used for the new decorated cache in the form of cache.getName() + "-" + cacheNameSuffix or cache.getName() if
     *         cacheNameSuffix is null
     */
    public static String generateDefaultDecoratedCacheName(Ehcache cache, String cacheNameSuffix) {
        if (cacheNameSuffix == null || cacheNameSuffix.trim().length() == 0) {
            return cache.getName();
        }
        return cache.getName() + DASH + cacheNameSuffix;
    }

}
