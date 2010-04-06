/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

/**
 * Interface that defines behaviors for different get/put/remove operations on a cache.
 * Different implementations can have different behaviors for each operation
 * 
 * @author Abhishek Sanoujam
 * 
 */
public interface NonStopCacheBehavior {

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param key
     * @return depends on the type extending this class
     * @throws IllegalStateException
     * @throws CacheException
     */
    public Element get(Object key) throws IllegalStateException, CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param key
     * @return
     * @throws IllegalStateException
     * @throws CacheException
     */
    public Element getQuiet(Object key) throws IllegalStateException, CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @return list of keys
     * @throws IllegalStateException
     * @throws CacheException
     */
    public List getKeys() throws IllegalStateException, CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @return list of keys
     * @throws IllegalStateException
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @return list of keys
     * @throws IllegalStateException
     * @throws CacheException
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param key
     * @return true if the key is in the cache otherwise false
     */
    public boolean isKeyInCache(Object key);

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param value
     * @return true if the value is in the cache otherwise false
     */
    public boolean isValueInCache(Object value);

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param element
     * @param doNotNotifyCacheReplicators
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
            CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param element
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param element
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param element
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param key
     * @param doNotNotifyCacheReplicators
     * @return
     * @throws IllegalStateException
     */
    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param key
     * @return
     * @throws IllegalStateException
     */
    public boolean remove(Object key) throws IllegalStateException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @throws IllegalStateException
     * @throws CacheException
     */
    public void removeAll() throws IllegalStateException, CacheException;

    /**
     * This method is called when the same operation times out in the TimeoutCache
     * 
     * @param doNotNotifyCacheReplicators
     * @throws IllegalStateException
     * @throws CacheException
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException;

}
