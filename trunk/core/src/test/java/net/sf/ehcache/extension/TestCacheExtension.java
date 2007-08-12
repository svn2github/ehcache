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

package net.sf.ehcache.extension;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.Element;

/**
 * Tests the interface methods of CacheExtension
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class TestCacheExtension implements CacheExtension {

    /**
     * Package local and static so we can test
     */
    private static Status status;

    /**
     * Package local and static so we can test
     */
    private static String propertyA;

    /**
     * Package local so we can test
     */
    private Ehcache cache;




    /**
     * Creates a cache extension. Note that an Ehcache is passed in. To do anything
     * useful a CacheExtension needs a cache reference.
     */
    public TestCacheExtension(Ehcache cache, String propertyA) {
        this.cache = cache;
        TestCacheExtension.propertyA = propertyA;
        status = Status.STATUS_UNINITIALISED;
    }


    /**
     * Notifies providers to initialise themselves.
     *
     * @throws net.sf.ehcache.CacheException
     */
    public void init() {
        status = Status.STATUS_ALIVE;
        cache.put(new Element("key1", "value1"));

    }

    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on
     * dispose.
     *
     * @throws net.sf.ehcache.CacheException
     */
    public void dispose() throws CacheException {
        status = Status.STATUS_SHUTDOWN;
    }

    /**
     *
     * @return
     */
    public Status getStatus() {
        return status;
    }

    /**
     *
     * @return
     */
    public static Status getStaticStatus() {
        return status;
    }


    /**
     *
     * @return
     */
    public static String getPropertyA() {
        return propertyA;
    }

    /**
     * 
     * @return
     */
    public Ehcache getCache() {
        return cache;
    }

    /**
     * Creates a clone of this extension. This method will only be called by ehcache before a
     * cache is initialized.
     * <p/>
     * Implementations should throw CloneNotSupportedException if they do not support clone but that
     * will stop them from being used with defaultCache.
     *
     * @return a clone
     * @throws CloneNotSupportedException if the extension could not be cloned.
     */
    public CacheExtension clone(Ehcache newCache) throws CloneNotSupportedException {
        TestCacheExtension copy = new TestCacheExtension(newCache, propertyA);
        return copy;
    }







}
