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

package net.sf.ehcache.constructs.readthrough;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.CacheDecoratorFactory;

/**
 * {@link CacheDecoratorFactory} implementation for the {@link ReadThroughCache} cache
 * decorator. the Properties arguments are used to configure {@link ReadThroughCacheConfiguration}
 * instances, which are in turn used to create the cache decorator instances.
 *
 * @author cschanck
 *
 */
public class ReadThroughCacheFactory extends CacheDecoratorFactory {

    @Override
    public Ehcache createDecoratedEhcache(Ehcache cache, Properties properties) {
        ReadThroughCacheConfiguration config = new ReadThroughCacheConfiguration().fromProperties(properties).build();
        return new ReadThroughCache(cache, config);
    }

    @Override
    public Ehcache createDefaultDecoratedEhcache(Ehcache cache, Properties properties) {
        ReadThroughCacheConfiguration config = new ReadThroughCacheConfiguration().fromProperties(properties).build();
        return new ReadThroughCache(cache, config);
    }

}
