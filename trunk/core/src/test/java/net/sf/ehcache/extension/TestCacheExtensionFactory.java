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

package net.sf.ehcache.extension;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;

/**
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class TestCacheExtensionFactory extends CacheExtensionFactory {

    /**
     * @param cache      the cache this extension should hold a reference to, and to whose lifecycle it should be bound.
     * @param properties implementation specific properties configured as delimiter separated name value pairs in ehcache.xml
     */
    public CacheExtension createCacheExtension(Ehcache cache, Properties properties) {
        String propertyA = PropertyUtil.extractAndLogProperty("propertyA", properties);
        return new TestCacheExtension(cache, propertyA);
    }
}
