/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.loader;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ComponentA is composed of ComponentB and some other fields. Tests the interactions between two loaders, where the first component's
 * loader loads component B by using getWithLoader, which in turn invokes component B's loader.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class CompositeLoaderTest {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeLoaderTest.class.getName());

    /**
     * Sets up the fixture, for example, open a network connection.
     * This method is called before a test is executed.
     */
    @Before
    public void setUp() throws Exception {
        CacheHelper.init();
    }

    /**
     * Tears down the fixture, for example, close a network connection.
     * This method is called after a test is executed.
     */
    @After
    public void tearDown() throws Exception {
        CacheHelper.shutdown();
    }

    /**
     * This test reproduces a deadlock found in 1.4-beta1 around loading interactions and getWithLoader. Now fixed.
     */
    @Test
    public void testCompositeLoad() {
        LOG.info("Getting from cache");
        ComponentA compA = (ComponentA) CacheHelper.get("ehcache-loaderinteractions.xml", "ACache", "key1");
        LOG.info(compA.toString());

    }

}
