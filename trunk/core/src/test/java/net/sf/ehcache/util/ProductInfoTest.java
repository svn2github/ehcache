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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author hhuynh
 */
public class ProductInfoTest {

    @Test
    public void testBuildInfo() {
        ProductInfo pi = new ProductInfo("/productinfotest-version.properties");
        assertEquals("Ehcache Core", pi.getName());
        assertEquals("1.7.0-SNAPSHOT", pi.getVersion());
        assertEquals("2009-09-18 02:15:49", pi.getBuildTime());
        assertEquals("1078", pi.getBuildRevision());
        assertEquals("1.6.0_16", pi.getBuildJdk());
        assertEquals("hhuynh", pi.getBuiltBy());

        assertEquals(
                "Ehcache Core version 1.7.0-SNAPSHOT was built on 2009-09-18 02:15:49, at revision 1078, with jdk 1.6.0_16 by hhuynh",
                pi.toString());
    }
}
