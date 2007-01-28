/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.jcache;

import net.sf.jsr107cache.CacheLoader;

import java.util.Map;

/**
 * A factory for creating counting cache loaders. This can be hooked up to the JCacheFactory by
 * specifying "cacheLoaderFactoryClassName" in the environment
 * @author Greg Luck
 * @version $Id$
 */
public class CountingCacheLoaderFactory extends CacheLoaderFactory {


    /**
     * Creates a CacheLoader. This method is called from {@link JCacheFactory}
     *
     * @param environment the same environment passed into {@link JCacheFactory}. This factory can
     *                    extract any properties it needs from the environment.
     * @return a constructed CacheLoader
     */
    public CacheLoader createCacheLoader(Map environment) {
        return new CountingCacheLoader();
    }
}
