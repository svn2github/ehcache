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

package net.sf.ehcache.bootstrap;

import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;

/**
 * An abstract factory for creating BootstrapCacheLoader instances. Implementers should provide their own
 * concrete factory extending this factory. It can then be configured in ehcache.xml.
 *
 * @param <T> The BootstrapCacheLoader type this Factory will create
 * @author Greg Luck
 * @version $Id$
 */
public abstract class BootstrapCacheLoaderFactory<T extends BootstrapCacheLoader> {

    /**
     * The property name expected in ehcache.xml for the bootstrap asyncrhonously switch.
     */
    public static final String BOOTSTRAP_ASYNCHRONOUSLY = "bootstrapAsynchronously";

    /**
     * Create a <code>BootstrapCacheLoader</code>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * @return a constructed BootstrapCacheLoader
     */
    public abstract T createBootstrapCacheLoader(Properties properties);

    /**
     * Extracts the value of bootstrapAsynchronously from the properties
     *
     * @param properties the properties passed by the CacheManager, read from the configuration file
     * @return true if to be bootstrapped asynchronously, false otherwise
     */
    protected boolean extractBootstrapAsynchronously(Properties properties) {
        return extractBoolean(properties, BOOTSTRAP_ASYNCHRONOUSLY, true);
    }

    /**
     * Will retrieve the boolean value from the properties, defaulting if property isn't present
     * @param properties the properties to use
     * @param prop the property name to look for
     * @param defaultValue the default value if property is missing
     * @return the value, or it's default, for the property
     */
    protected boolean extractBoolean(final Properties properties, final String prop, final boolean defaultValue) {
        boolean value;
        String propString = PropertyUtil.extractAndLogProperty(prop, properties);
        if (propString != null) {
            value = PropertyUtil.parseBoolean(propString);
        } else {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Will retrieve the boolean value from the properties, defaulting if property isn't present
     * @param properties the properties to use
     * @param prop the property name to look for
     * @param defaultValue the default value if property is missing
     * @return the value, or it's default, for the property
     */
    protected long extractLong(final Properties properties, final String prop, final long defaultValue) {
        long value;
        String propString = PropertyUtil.extractAndLogProperty(prop, properties);
        if (propString != null) {
            value = Long.parseLong(propString);
        } else {
            value = defaultValue;
        }
        return value;
    }
}
