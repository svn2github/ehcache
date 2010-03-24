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

package net.sf.ehcache.util;

import org.junit.Test;

/**
 * 
 * @author hhuynh
 *
 */
public class UpdateCheckerTest {
    
    @Test
    public void testErrorNotBubleUp() {
        // make sure the test won't skip update check
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "false");
        
        // use a bad url
        System.setProperty("ehcache.update-check.url", "this is a bad url");
        
        UpdateChecker uc = new UpdateChecker();
        uc.checkForUpdate();
        
        // update check will fail (warning in log) but should not throw any exception
    }
}
