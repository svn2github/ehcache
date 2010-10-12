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
 * <p>
 * It is mandatory to specify properties when configuring {@link NonStopCacheDecoratorFactory} in ehcache.xml. List of all the properties
 * supported by {@link NonStopCacheDecoratorFactory} and corresponding valid values are:
 * <ul>
 * <li>name : any string name for the NonStopCache. This property is mandatory.</li>
 * <li>timeoutMillis : Any number for use as the timeout time in milliseconds before timing out for any operation. After operation times
 * out, behavior as specified by <tt>timeoutBehavior</tt> happens. This property is optional and uses a default value if not specified.</li>
 * <li>timeoutBehavior : {exception | noop | localReads}. This property is optional and uses a default value if not specified.</li>
 * <li>immediateTimeout = {true | false}. This property is optional and uses a default value if not specified.</li>
 * </ul>
 * The default values for the optional properties are:
 * <ul>
 * <li>timeoutMillis = 5000</li>
 * <li>timeoutBehavior = exception</li>
 * <li>immediateTimeout = true</li>
 * </ul>
 * Example sample config:
 * 
 * <pre>
 * &lt;cache>
 * ...
 * &lt;cacheDecoratorFactory
 * class="net.sf.ehcache.constructs.nonstop.NonStopCacheDecoratorFactory"
 * properties="name=myNonStopCacheName, timeoutMillis=1000, timeoutBehavior=exception, immediateTimeout=true" />
 * ...
 * &lt;/cache>
 * </pre>
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
            name = cache.getName();
        }
        return new NonStopCache(cache, name, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Ehcache createDefaultDecoratedEhcache(Ehcache cache, Properties properties) {
        if (properties == null) {
            throw new CacheException(NonStopCacheDecoratorFactory.class.getName() + " cannot be used without any configuration properties");
        }
        return new NonStopCache(cache, generateDefaultDecoratedCacheName(cache, properties.getProperty("name")), properties);
    }

}
