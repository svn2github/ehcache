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

package net.sf.ehcache.util;

import net.sf.ehcache.CacheTest;
import net.sf.ehcache.ElementTest;
import net.sf.ehcache.constructs.blocking.BlockingCacheTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This test suite will run a bunch of tests that you specify
 * Good test to run if you want to do a quick smoke tests of ehcache
 * <p/>
 * mvn test -Dtest=SmokeTests
 *
 * @author hhuynh
 */
@RunWith (Suite.class)
@Suite.SuiteClasses({ CacheTest.class, ElementTest.class, BlockingCacheTest.class })
public class SmokeTests {

    public SmokeTests() {
        int i = 0;
    }
}
