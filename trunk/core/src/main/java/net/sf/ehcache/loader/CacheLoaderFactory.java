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

package net.sf.ehcache.loader;

import net.sf.ehcache.Ehcache;

import java.util.Properties;

/**
 * An abstract factory for creating cache loaders. Implementers should provide their own
 * concrete factory extending this factory.
 * <p/>
 * Note that Ehcache API also allows the CacheLoader to be set programmatically.
 * @author Greg Luck
 * @version $Id$
 */
public abstract class CacheLoaderFactory {

    /**
     * Creates a CacheLoader using the Ehcache configuration mechanism at the time the associated cache
     *  is created.
     * @param cache a reference to the owning cache
     * @param properties implementation specific properties configured as delimiter
     *  separated name value pairs in ehcache.xml
     * @return a constructed CacheLoader
     */
    public abstract CacheLoader createCacheLoader(Ehcache cache, Properties properties);

}
