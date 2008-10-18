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

package net.sf.ehcache.loader;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.jsr107cache.CacheException;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;


/**
 * Written for Dead-lock poc
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class BaseComponentLoader extends CacheLoaderFactory implements CacheLoader {

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
    public net.sf.ehcache.loader.CacheLoader createCacheLoader(Ehcache cache, Properties properties) {
        this.props = properties;
        return this;
    }

    /**
     * @param arg0
     * @return
     * @throws CacheException
     */
    public Object load(Object arg0) throws CacheException {
        return load(arg0, null);
    }

    /**
     * @param arg0
     * @param argument
     * @return
     * @throws CacheException
     */
    public Object load(Object arg0, Object argument) throws CacheException {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * @param keys
     * @return
     * @throws CacheException
     */
    public Map loadAll(Collection keys) throws CacheException {
        return loadAll(keys, null);
    }

    /**
     * @param keys
     * @param argument
     * @return
     * @throws CacheException
     */
    public Map loadAll(Collection keys, Object argument) throws CacheException {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * Gets the name of a CacheLoader
     *
     * @return the name of this CacheLoader
     */
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Creates a clone of this extension. This method will only be called by ehcache before a
     * cache is initialized.
     * <p/>
     * Implementations should throw CloneNotSupportedException if they do not support clone
     * but that will stop them from being used with defaultCache.
     *
     * @return a clone
     * @throws CloneNotSupportedException if the extension could not be cloned.
     */
    public CacheLoader clone(Ehcache cache) throws CloneNotSupportedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Notifies providers to initialise themselves.
     * <p/>
     * This method is called during the Cache's initialise method after it has changed it's
     * status to alive. Cache operations are legal in this method.
     *
     * @throws net.sf.ehcache.CacheException
     */
    public void init() {
        //nothing required
    }

    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on
     * dispose.
     * <p/>
     * Cache operations are illegal when this method is called. The cache itself is partly
     * disposed when this method is called.
     *
     * @throws net.sf.ehcache.CacheException
     */
    public void dispose() throws net.sf.ehcache.CacheException {
        //no op
    }

    /**
     * @return the status of the extension
     */
    public Status getStatus() {
        return null;
    }

}
