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

import java.io.Serializable;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;

/**
 * Interface that defines behaviors for different get/put/remove operations on a cache.
 * Different implementations can have different behaviors for each operation
 * 
 * Methods in this interface contains those methods in Ehcache that can reach the Terracotta layer
 *  (and potentially block when the cluster goes offline)
 * 
 * @author Abhishek Sanoujam
 * 
 */
public interface NonStopCacheBehavior {

    /**
     * Method defining the {@link #get(Object)} behavior
     * 
     * @param key
     * @return depends on the type implementing this class
     * @throws IllegalStateException
     * @throws CacheException
     */
    Element get(Object key) throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #get(Serializable)} behavior
     * 
     * @param key
     * @return the value associated for the key
     * @throws IllegalStateException
     * @throws CacheException
     */
    Element get(final Serializable key) throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #getQuiet(Object)} behavior
     * 
     * @param key
     * @return the value associated for the key
     * @throws IllegalStateException
     * @throws CacheException
     */
    Element getQuiet(Object key) throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #getQuiet(Serializable)} behavior
     * 
     * @param key
     * @return the value associated for the key
     * @throws IllegalStateException
     * @throws CacheException
     */
    Element getQuiet(final Serializable key) throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #getKeys()} behavior
     * 
     * @return list of keys
     * @throws IllegalStateException
     * @throws CacheException
     */
    List getKeys() throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #getKeysNoDuplicateCheck()} behavior
     * 
     * @return list of keys
     * @throws IllegalStateException
     */
    List getKeysNoDuplicateCheck() throws IllegalStateException;

    /**
     * Method defining the {@link #getKeysWithExpiryCheck()} behavior
     * 
     * @return list of keys
     * @throws IllegalStateException
     * @throws CacheException
     */
    List getKeysWithExpiryCheck() throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #getSizeBasedOnAccuracy(int)} behavior
     * 
     * @param statisticsAccuracy
     * @return the size of the cache based on the accuracy setting
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException;

    /**
     * Method defining the {@link #isKeyInCache(Object)} behavior
     * 
     * @param key
     * @return true if the key is in the cache otherwise false
     */
    boolean isKeyInCache(Object key);

    /**
     * Method defining the {@link #isValueInCache(Object)} behavior
     * 
     * @param value
     * @return true if the value is in the cache otherwise false
     */
    boolean isValueInCache(Object value);

    /**
     * Method defining the {@link #put(Element, boolean)} behavior
     * 
     * @param element
     * @param doNotNotifyCacheReplicators
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
            CacheException;

    /**
     * Method defining the {@link #put(Element)} behavior
     * 
     * @param element
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException;

    /**
     * Method defining the {@link #putQuiet(Element)} behavior
     * 
     * @param element
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException;

    /**
     * Method defining the {@link #putWithWriter(Element)} behavior
     * 
     * @param element
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws CacheException
     */
    void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException;

    /**
     * Method defining the {@link #remove(Object, boolean)} behavior
     * 
     * @param key
     * @param doNotNotifyCacheReplicators
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException;

    /**
     * Method defining the {@link #remove(Object)} behavior
     * 
     * @param key
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    boolean remove(Object key) throws IllegalStateException;

    /**
     * Method defining the {@link #remove(Serializable, boolean)} behavior
     * 
     * @param key
     * @param doNotNotifyCacheReplicators
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    boolean remove(final Serializable key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException;

    /**
     * Method defining the {@link #remove(Serializable)} behavior
     * 
     * @param key
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    boolean remove(final Serializable key) throws IllegalStateException;

    /**
     * Method defining the {@link #removeQuiet(Object)} behavior
     * 
     * @param key
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    boolean removeQuiet(Object key) throws IllegalStateException;

    /**
     * Method defining the {@link #removeQuiet(Serializable)} behavior
     * 
     * @param key
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    boolean removeQuiet(Serializable key) throws IllegalStateException;

    /**
     * Method defining the {@link #removeAll()} behavior
     * 
     * @throws IllegalStateException
     * @throws CacheException
     */
    void removeAll() throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #removeAll()} behavior
     * 
     * @param doNotNotifyCacheReplicators
     * @throws IllegalStateException
     * @throws CacheException
     */
    void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #removeWithWriter(Object)} behavior
     * 
     * @param key
     * @return depends on the implementation
     * @throws IllegalStateException
     * @throws CacheException
     */
    boolean removeWithWriter(Object key) throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #getInternalContext()} behavior
     * 
     * @return depends on the implementation
     */
    Object getInternalContext();

    /**
     * Method defining the {@link #getStatistics()} behavior
     * 
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    Statistics getStatistics() throws IllegalStateException;

    /**
     * Method defining the {@link #calculateInMemorySize()} behavior
     * 
     * @return depends on the implementation
     * @throws IllegalStateException
     * @throws CacheException
     */
    long calculateInMemorySize() throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #getMemoryStoreSize()} behavior
     * 
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    long getMemoryStoreSize() throws IllegalStateException;

    /**
     * Method defining the {@link #getDiskStoreSize()} behavior
     * 
     * @return depends on the implementation
     * @throws IllegalStateException
     */
    int getDiskStoreSize() throws IllegalStateException;

    /**
     * Method defining the {@link #isElementInMemory(Serializable)} behavior
     * 
     * @param key
     * @return depends on the implementation
     */
    boolean isElementInMemory(Serializable key);

    /**
     * Method defining the {@link #isElementInMemory(Object)} behavior
     * 
     * @param key
     * @return depends on the implementation
     */
    boolean isElementInMemory(Object key);

    /**
     * Method defining the {@link #isElementOnDisk(Serializable)} behavior
     * 
     * @param key
     * @return depends on the implementation
     */
    boolean isElementOnDisk(Serializable key);

    /**
     * Method defining the {@link #isElementOnDisk(Object)} behavior
     * 
     * @param key
     * @return depends on the implementation
     */
    boolean isElementOnDisk(Object key);

    /**
     * Method defining the {@link #evictExpiredElements()} behavior
     */
    void evictExpiredElements();

    /**
     * Method defining the {@link #replace(Element, Element)} behavior
     * 
     * @param old
     * @param element
     * @return depends on the implementation
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException;

    /**
     * Method defining the {@link #replace(Element)} behavior
     * 
     * @param element
     * @return depends on the implementation
     * @throws NullPointerException
     */
    Element replace(Element element) throws NullPointerException;

    /**
     * Method defining the {@link #flush()} behavior
     * 
     * @throws IllegalStateException
     * @throws CacheException
     */
    void flush() throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #getSize()} behavior
     * 
     * @return depends on the implementation
     * @throws IllegalStateException
     * @throws CacheException
     */
    int getSize() throws IllegalStateException, CacheException;

    /**
     * Method defining the {@link #removeElement(Element)} behavior
     * 
     * @param element
     * @return depends on the implementation
     * @throws NullPointerException
     */
    boolean removeElement(Element element) throws NullPointerException;

    /**
     * Method defining the {@link #putIfAbsent(Element)} behavior
     * 
     * @param element
     * @return depends on the implementation
     * @throws NullPointerException
     */
    Element putIfAbsent(Element element) throws NullPointerException;

}
