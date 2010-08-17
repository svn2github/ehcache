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

import junit.framework.TestCase;

/**
 * Test for {@link NonStopCacheConfig}
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class NonStopCacheConfigTest extends TestCase {

    /**
     * test default values
     */
    public void testDefaults() {
        NonStopCacheConfig config = new NonStopCacheConfigImpl();
        assertEquals(NonStopCacheConfig.DEFAULT_TIMEOUT_MILLIS, config.getTimeoutMillis());
        assertEquals(NonStopCacheConfig.DEFAULT_IMMEDIATE_TIMEOUT, config.isImmediateTimeout());
        assertEquals(NonStopCacheConfig.DEFAULT_TIMEOUT_BEHAVIOR_TYPE, config.getTimeoutBehaviorType());
    }

    /**
     * test constructor with properties
     */
    public void testConstructorWithProperties() {

        String[] immediateTimeoutValues = new String[] { "true", "false" };
        boolean[] expectedImmediateTimeouts = new boolean[] { true, false };
        String[] timeoutBehaviorValues = new String[] { "exception", "noop", "localReads" };
        NonStopCacheBehaviorType[] expectedTimeoutBehaviorTypes = new NonStopCacheBehaviorType[] {
                NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT, NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT,
                NonStopCacheBehaviorType.LOCAL_READS_ON_TIMEOUT };

        Properties props = new Properties();
        props.setProperty(NonStopCacheConfig.TIMEOUT_MILLIS_PROP_KEY, "9245");
        for (int i = 0; i < immediateTimeoutValues.length; i++) {
            for (int j = 0; j < timeoutBehaviorValues.length; j++) {
                props.setProperty(NonStopCacheConfig.IMMEDIATE_TIMEOUT_PROP_KEY, immediateTimeoutValues[i]);
                props.setProperty(NonStopCacheConfig.TIMEOUT_BEHAVIOR_PROP_KEY, timeoutBehaviorValues[j]);
                NonStopCacheConfig config = new NonStopCacheConfigImpl(props);
                assertEquals(9245, config.getTimeoutMillis());
                assertEquals(expectedImmediateTimeouts[i], config.isImmediateTimeout());
                assertEquals(expectedTimeoutBehaviorTypes[j], config.getTimeoutBehaviorType());
            }
        }
    }

    /**
     * Test dynamic change of config
     */
    public void testDynamicChange() {
        NonStopCacheConfig config = new NonStopCacheConfigImpl();
        config.setTimeoutMillis(1245);
        assertEquals(1245, config.getTimeoutMillis());

        config.setImmediateTimeout(false);
        assertFalse(config.isImmediateTimeout());

        config.setImmediateTimeout(true);
        assertTrue(config.isImmediateTimeout());

        config.setTimeoutBehaviorType(NonStopCacheBehaviorType.LOCAL_READS_ON_TIMEOUT);
        assertEquals(NonStopCacheBehaviorType.LOCAL_READS_ON_TIMEOUT, config.getTimeoutBehaviorType());

        config.setTimeoutBehaviorType(NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        assertEquals(NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT, config.getTimeoutBehaviorType());

        config.setTimeoutBehaviorType(NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT);
        assertEquals(NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT, config.getTimeoutBehaviorType());
    }

}
