/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

package net.sf.ehcache.spring;

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Integration tests to make sure we do not break Spring
 *
 * @author Greg Luck
 * @version $Id$
 */
public class SpringIntegrationTest extends AbstractDependencyInjectionSpringContextTests {

    /**
     * Tell Spring to use protecteds
     */
    public SpringIntegrationTest() {
        setPopulateProtectedVariables(true);
    }


    /**
     * (non-Javadoc)
     *
     * @see org.springframework.test.AbstractSingleSpringContextTests#getConfigLocations()
     */
    protected String[] getConfigLocations() {
        return new String[]{"classpath:/spring/ehcache-beans.xml", };
    }


    /**
     * Can we create a CacheManager through Spring
     */
    public void testCacheManager() {

        //Right now this loads the Spring context and creates a CacheManager

    }

}
