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

package net.sf.ehcache.constructs.unlockedreadsview;

import java.util.Properties;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.CacheDecoratorFactory;

/**
 * Concrete factory for creating {@link UnlockedReadsView} using ehcache.xml
 * <p>
 * {@link UnlockedReadsViewDecoratorFactory} needs to be configured in ehcache.xml with a mandatory "name" property which will be used as
 * the name of the new {@link UnlockedReadsView}. e.g.
 * 
 * <pre>
 * &lt;cache>
 * ...
 * &lt;cacheDecoratorFactory
 * class="net.sf.ehcache.constructs.unlockedreadsview.UnlockedReadsViewDecoratorFactory"
 * properties="name=myUnlockedReadsViewName" />
 * ...
 * &lt;/cache>
 * </pre>
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class UnlockedReadsViewDecoratorFactory extends CacheDecoratorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Ehcache createDecoratedEhcache(Ehcache ehcache, Properties properties) {
        if (!(ehcache instanceof Cache)) {
            throw new CacheException(UnlockedReadsViewDecoratorFactory.class.getName() + " can only be used to decorate "
                    + Cache.class.getName() + " instances.");
        }
        Cache cache = (Cache) ehcache;
        if (properties == null) {
            throw new CacheException(UnlockedReadsViewDecoratorFactory.class.getName()
                    + " cannot be used without any configuration properties");
        }
        String name = properties.getProperty("name");
        if (name == null || name.trim().length() == 0) {
            throw new CacheException(UnlockedReadsViewDecoratorFactory.class.getName()
                    + " needs to be configured with a mandatory 'name' property");
        }
        return new UnlockedReadsView(cache, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Ehcache createDefaultDecoratedEhcache(Ehcache cache, Properties properties) {
        if (properties == null) {
            throw new CacheException(UnlockedReadsViewDecoratorFactory.class.getName()
                    + " cannot be used without any configuration properties");
        }
        return new UnlockedReadsView((Cache) cache, generateDefaultDecoratedCacheName(cache, properties.getProperty("name")));
    }

}
