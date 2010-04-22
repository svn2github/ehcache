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

package net.sf.ehcache.constructs.nonstop;

import java.util.Properties;

/**
 * Implementation of {@link NonStopCacheConfig}
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class NonStopCacheConfigImpl implements NonStopCacheConfig {

    private static final Properties DEFAULT_VALUES_PROPERTIES = new Properties();

    private long timeoutMillis;
    private boolean immediateTimeout;
    private NonStopCacheBehaviorType timeoutBehaviorType;

    /**
     * Default constructor. Uses default values defined in {@link NonStopCacheConfig}
     */
    public NonStopCacheConfigImpl() {
        this(DEFAULT_VALUES_PROPERTIES);
    }

    /**
     * Constructor accepting a {@link Properties} that contains mappings for the {@link NonStopCacheConfig}. See {@link NonStopCacheConfig}
     * for allowed key and values
     * 
     * @param properties
     */
    public NonStopCacheConfigImpl(final Properties properties) {
        this.timeoutMillis = getInt(properties, TIMEOUT_MILLIS_PROP_KEY);
        this.timeoutBehaviorType = NonStopCacheBehaviorType
                .getTypeFromConfigPropertyName(properties.getProperty(TIMEOUT_BEHAVIOR_PROP_KEY));
        this.immediateTimeout = getBoolean(properties, IMMEDIATE_TIMEOUT_PROP_KEY);
    }

    static {
        DEFAULT_VALUES_PROPERTIES.setProperty(TIMEOUT_MILLIS_PROP_KEY, "" + DEFAULT_TIMEOUT_MILLIS);
        DEFAULT_VALUES_PROPERTIES.setProperty(TIMEOUT_BEHAVIOR_PROP_KEY, DEFAULT_TIMEOUT_BEHAVIOR_TYPE.getConfigPropertyName());
        DEFAULT_VALUES_PROPERTIES.setProperty(IMMEDIATE_TIMEOUT_PROP_KEY, "" + DEFAULT_IMMEDIATE_TIMEOUT);
    }

    private static boolean getBoolean(final Properties properties, final String key) {
        String value = properties.getProperty(key);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            throw new IllegalArgumentException("Value for '" + key + "' should be either 'true' or 'false' -- " + value);
        }
    }

    private static int getInt(final Properties properties, final String key) {
        String value = properties.getProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Specified value for '" + key + "' is not a number - " + value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * {@inheritDoc}
     */
    public void setTimeoutMillis(final long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isImmediateTimeout() {
        return immediateTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public void setImmediateTimeout(final boolean immediateTimeout) {
        this.immediateTimeout = immediateTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public NonStopCacheBehaviorType getTimeoutBehaviorType() {
        return this.timeoutBehaviorType;
    }

    /**
     * {@inheritDoc}
     */
    public void setTimeoutBehaviorType(final NonStopCacheBehaviorType timeoutBehaviorType) {
        this.timeoutBehaviorType = timeoutBehaviorType;
    }

}
