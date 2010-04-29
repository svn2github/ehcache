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

package net.sf.ehcache.constructs.nonstop;

import java.util.Properties;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.CacheDecoratorFactory;

/**
 * Concrete factory for creating {@link NonStopCache} decorators using ehcache.xml
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class NonStopCacheDecoratorFactory extends CacheDecoratorFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Ehcache createDecoratedEhcache(Ehcache cache, Properties properties) {
        if (properties == null) {
            throw new CacheException(NonStopCacheDecoratorFactory.class.getName() + " cannot be used without any configuration properties");
        }
        String name = properties.getProperty("name");
        if (name == null || name.trim().length() == 0) {
            throw new CacheException("NonStopCacheDecoratorFactory needs to be configured with a mandatory 'name' property");
        }
        return new NonStopCache(cache, name, properties);
    }

}
