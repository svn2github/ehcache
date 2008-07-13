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

package net.sf.ehcache.loader;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import net.sf.jsr107cache.CacheException;


/**
 * Written for Dead-lock poc
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public abstract class BaseComponentLoader extends CacheLoaderFactory implements CacheLoader {

    /**
     *
     */
    protected Properties props;

    /**
     * Create a JSR107 cache loader
     */
    public net.sf.jsr107cache.CacheLoader createCacheLoader(Map arg0) {
        Properties p = new Properties();
        p.putAll(arg0);
        return createCacheLoader(p);
    }

    /**
     * create a ehCache Cache loader (which extends jsr107 cache loader)
     */
    public net.sf.ehcache.loader.CacheLoader createCacheLoader(Properties properties) {
        this.props = properties;
        return this;
    }

    /**
     *
     * @param arg0
     * @return
     * @throws CacheException
     */
    public Object load(Object arg0) throws CacheException {
        return load(arg0, null);
    }

    /**
     *
     * @param arg0
     * @param argument
     * @return
     * @throws CacheException
     */
    public Object load(Object arg0, Object argument) throws CacheException {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     *
     * @param keys
     * @return
     * @throws CacheException
     */
    public Map loadAll(Collection keys) throws CacheException {
        return loadAll(keys, null);
    }

    /**
     *
     * @param keys
     * @param argument
     * @return
     * @throws CacheException
     */
    public Map loadAll(Collection keys, Object argument) throws CacheException {
        throw new UnsupportedOperationException("Method not implemented");
    }

}
