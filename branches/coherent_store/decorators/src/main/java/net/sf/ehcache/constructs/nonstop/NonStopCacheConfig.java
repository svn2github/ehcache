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

public interface NonStopCacheConfig {

    public static final long DEFAULT_TIMEOUT_VALUE_IN_MILLIS = 1000;

    public static final NonStopCacheBehaviorType DEFAULT_TIMEOUT_BEHAVIOR_TYPE = NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT;

    public static final boolean DEFAULT_FAIL_FAST = true;

    public static final String TIMEOUT_VALUE_IN_MILLIS_PROP_KEY = "timeoutValueInMillis";

    public static final String TIMEOUT_BEHAVIOR_PROP_KEY = "timeoutBehavior";

    public static final String FAIL_FAST_PROP_KEY = "failFast";

    public long getTimeoutValueInMillis();

    public void setTimeoutValueInMillis(long timeoutValueInMillis);

    public void setFailFast(boolean failFast);

    public boolean isFailFast();

    public void setTimeoutBehaviorType(NonStopCacheBehaviorType timeoutBehaviorType);

    public NonStopCacheBehaviorType getTimeoutBehaviorType();

}
