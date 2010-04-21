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

/**
 * Interface for NonStopCache's configuration
 * 
 * @author Abhishek Sanoujam
 * 
 */
public interface NonStopCacheConfig {

    /**
     * Default value for timeoutMillis property
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = 5000;

    /**
     * Default {@link NonStopCacheBehaviorType}
     */
    public static final NonStopCacheBehaviorType DEFAULT_TIMEOUT_BEHAVIOR_TYPE = NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT;

    /**
     * Default value for immediateTimeout property
     */
    public static final boolean DEFAULT_IMMEDIATE_TIMEOUT = true;

    /**
     * Key to be used for configuring timeoutMillis property when using java.util.Properties to configure NonStopCache
     */
    public static final String TIMEOUT_MILLIS_PROP_KEY = "timeoutMillis";

    /**
     * Key to be used for configuring timeoutBehavior property when using java.util.Properties to configure NonStopCache. Values accepted
     * are {@link NonStopCacheBehaviorType#EXCEPTION_CONFIG_PROPERTY_NAME}, {@link NonStopCacheBehaviorType#NO_OP_CONFIG_PROPERTY_NAME} and
     * {@link NonStopCacheBehaviorType#LOCAL_READS_CONFIG_PROPERTY_NAME}
     */
    public static final String TIMEOUT_BEHAVIOR_PROP_KEY = "timeoutBehavior";

    /**
     * Key to be used for configuring immediateTimeout property when using java.util.Properties to configure NonStopCache.
     */
    public static final String IMMEDIATE_TIMEOUT_PROP_KEY = "immediateTimeout";

    /**
     * returns the value of timeoutMillis
     * 
     * @return the value of timeoutMillis
     */
    public long getTimeoutMillis();

    /**
     * Set the value of timeoutMillis
     * 
     * @param timeoutMillis
     */
    public void setTimeoutMillis(long timeoutMillis);

    /**
     * Set the value of immediateTimeout
     * 
     * @param immediateTimeout
     */
    public void setImmediateTimeout(boolean immediateTimeout);

    /**
     * Returns value of immediateTimeout
     * 
     * @return value of immediateTimeout
     */
    public boolean isImmediateTimeout();

    /**
     * Sets the value of {@link NonStopCacheBehaviorType}
     * 
     * @param timeoutBehaviorType
     */
    public void setTimeoutBehaviorType(NonStopCacheBehaviorType timeoutBehaviorType);

    /**
     * Returns the value of {@link NonStopCacheBehaviorType}
     * 
     * @return the value of {@link NonStopCacheBehaviorType}
     */
    public NonStopCacheBehaviorType getTimeoutBehaviorType();

}
