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

import java.io.Serializable;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;

/**
 * This class implements the simplest of all possible read through cache
 * behaviors, where a call the get() will delegate to a call to getWithLoader().
 * This means that a get() call can take a long time; beware. It also does no more
 * locking than {@link Cache.java} implements; each separate cache may try to load
 * a key at the same time.
 *
 * @author cschanck
 *
 */
public class ReadThroughCache extends EhcacheDecoratorAdapter {

    private final ReadThroughCacheConfiguration readThroughCacheConfig;
    private final boolean isModeGet;

    /**
     *
     * @param underlyingCache
     * @param config
     */
    public ReadThroughCache(Ehcache underlyingCache, ReadThroughCacheConfiguration config) {
        super(underlyingCache);
        this.readThroughCacheConfig = config;
        this.isModeGet = readThroughCacheConfig.isModeGet();
    }

    @Override
    public Element get(Object key) throws IllegalStateException, CacheException {
        if (isModeGet) {
            return super.getWithLoader(key, null, null);
        }
        return super.get(key);
    }

    @Override
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        if (isModeGet) {
            return super.getWithLoader(key, null, null);
        }
        return super.get(key);
    }

    @Override
    public String getName() {
        if (readThroughCacheConfig.getName() != null) {
            return readThroughCacheConfig.getName();
        }
        return super.getName();
    }

}
