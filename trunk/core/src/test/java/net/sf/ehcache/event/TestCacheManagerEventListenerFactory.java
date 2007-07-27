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

package net.sf.ehcache.event;

import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;

/**
 * A factory which creates a counter, for testing purposes
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class TestCacheManagerEventListenerFactory extends CacheManagerEventListenerFactory {
    /**
     * Create a <code>CacheEventListener</code>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * @return a constructed CacheManagerEventListener
     */
    public CacheManagerEventListener createCacheManagerEventListener(Properties properties) {
        String type = PropertyUtil.extractAndLogProperty("type", properties);
        if (type.equals("null") || type.equals("null")) {
            return null;
        } else if (type.equals("counting")) {
            return new CountingCacheManagerEventListener();
        } else {
            return null;
        }
    }
}
