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

package net.sf.ehcache.constructs.nonstop;

import java.util.Properties;

public class NonStopCacheConfigImpl implements NonStopCacheConfig {

    private long timeoutValueInMillis;
    private boolean failFast;
    private NonStopCacheBehaviorType timeoutBehaviorType;

    private static boolean getBoolean(Properties properties, String key) {
        String value = properties.getProperty(key);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            throw new IllegalArgumentException("Value for '" + key + "' should be either 'true' or 'false' -- " + value);
        }
    }

    private static int getInt(Properties properties, String key) {
        String value = properties.getProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Specified value for '" + key + "' is not a number - " + value);
        }
    }

    public NonStopCacheConfigImpl() {
        initialize(DEFAULT_TIMEOUT_VALUE_IN_MILLIS, DEFAULT_TIMEOUT_BEHAVIOR_TYPE, DEFAULT_FAIL_FAST);
    }

    public NonStopCacheConfigImpl(Properties properties) {
        initialize(getInt(properties, TIMEOUT_VALUE_IN_MILLIS_PROP_KEY), NonStopCacheBehaviorType.valueOf(properties
                .getProperty(TIMEOUT_BEHAVIOR_PROP_KEY)), getBoolean(properties, FAIL_FAST_PROP_KEY));
    }

    private void initialize(long timeoutValueInMillis, NonStopCacheBehaviorType timeoutBehaviorType, boolean failFast) {
        // do not access members directly, use set methods
        // to set the behavior correctly
        this.setTimeoutValueInMillis(timeoutValueInMillis);
        this.setTimeoutBehaviorType(timeoutBehaviorType);
        this.setFailFast(failFast);
    }

    public long getTimeoutValueInMillis() {
        return timeoutValueInMillis;
    }

    public void setTimeoutValueInMillis(long timeoutValueInMillis) {
        this.timeoutValueInMillis = timeoutValueInMillis;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public NonStopCacheBehaviorType getTimeoutBehaviorType() {
        return this.timeoutBehaviorType;
    }

    public void setTimeoutBehaviorType(NonStopCacheBehaviorType timeoutBehaviorType) {
        this.timeoutBehaviorType = timeoutBehaviorType;
    }

}
