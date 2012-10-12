/**
 *  Copyright Terracotta, Inc.
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
package net.sf.ehcache.constructs.scheduledrefresh;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.extension.CacheExtensionFactory;

import java.util.Properties;

/**
 * Factory class for generating instances of
 * {@link ScheduledRefreshCacheExtension}. This is the class used in the
 * ehcache.xml file to add and extension to a cache.
 *
 * @author cschanck
 */
public class ScheduledRefreshCacheExtensionFactory extends CacheExtensionFactory {
    /**
     * No arg constructor.
     */
    public ScheduledRefreshCacheExtensionFactory() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheExtension createCacheExtension(Ehcache cache, Properties properties) {
        ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration().fromProperties(properties).build();
        return new ScheduledRefreshCacheExtension(config, cache);
    }

}
