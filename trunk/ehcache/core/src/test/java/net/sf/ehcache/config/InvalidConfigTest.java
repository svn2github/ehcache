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

package net.sf.ehcache.config;

import junit.framework.TestCase;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

public class InvalidConfigTest extends TestCase {

    public void testInvalidBooleanAttribute() {
        try {
            CacheManager cacheManager = new CacheManager(InvalidConfigTest.class.getResourceAsStream("/invalid-config-test.xml"));
            fail("Should have failed as invalid value for boolean type attribute used in config");
        } catch (CacheException e) {
            if (!e.getMessage().contains("Invalid value specified for attribute 'coherent'")) {
                throw e;
            }
        }
    }

}
