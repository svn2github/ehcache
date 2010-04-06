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

package net.sf.ehcache.constructs.nonstop.behavior;

import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

/**
 * Implementation of {@link NonStopCacheBehavior} which throws {@link NonStopCacheException} for all operations.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class ExceptionOnTimeoutBehavior implements NonStopCacheBehavior {

    /**
     * the singleton instance
     */
    private static final ExceptionOnTimeoutBehavior INSTANCE = new ExceptionOnTimeoutBehavior();

    /**
     * Returns the singleton instance
     * 
     * @return the singleton instance
     */
    public static ExceptionOnTimeoutBehavior getInstance() {
        return INSTANCE;
    }

    /**
     * private constructor
     */
    private ExceptionOnTimeoutBehavior() {
        //
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("get for key - '" + key + "'  timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getQuite for key - '" + key + "'  timed out");
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getKeys timed out");
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        throw new NonStopCacheException("getKeysNoDuplicateCheck timed out");
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getKeysWithExpiryCheck timed out");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKeyInCache(Object key) {
        throw new NonStopCacheException("isKeyInCache timed out");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValueInCache(Object value) {
        throw new NonStopCacheException("isValueInCache timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        throw new NonStopCacheException("put for element - '" + element + "', doNotNotifyCacheReplicators - '"
                + doNotNotifyCacheReplicators + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("put for element - '" + element + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("putQuiet for element - '" + element + "' timed out");

    }

    /**
     * {@inheritDoc}
     */
    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("putWithWriter for element - '" + element + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        throw new NonStopCacheException("remove for key - '" + key + "', doNotNotifyCacheReplicators - '" + doNotNotifyCacheReplicators
                + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key) throws IllegalStateException {
        throw new NonStopCacheException("remove for key - '" + key + "' timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeAll timed out");
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeAll with doNotNotifyCacheReplicators - '" + doNotNotifyCacheReplicators + "' timed out");
    }

}
