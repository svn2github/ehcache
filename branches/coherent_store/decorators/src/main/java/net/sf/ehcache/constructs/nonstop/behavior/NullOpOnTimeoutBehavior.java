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

import java.util.Collections;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;

/**
 * Implementation of {@link NonStopCacheBehavior} which returns null for all get
 * operations and does nothing for puts and removes.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class NullOpOnTimeoutBehavior implements NonStopCacheBehavior {

    /**
     * the singleton instance
     */
    private static final NullOpOnTimeoutBehavior INSTANCE = new NullOpOnTimeoutBehavior();

    /**
     * Returns the singleton instance
     * 
     * @return the singleton instance
     */
    public static NullOpOnTimeoutBehavior getInstance() {
        return INSTANCE;
    }

    /**
     * private constructor
     */
    private NullOpOnTimeoutBehavior() {
        //
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) throws IllegalStateException, CacheException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        return null;
    }

    public List getKeys() throws IllegalStateException, CacheException {
        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKeyInCache(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValueInCache(Object value) {
        return false;
    }

    /**
     * {@inheritDoc}
     */

    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key) throws IllegalStateException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        // no-op
    }

}