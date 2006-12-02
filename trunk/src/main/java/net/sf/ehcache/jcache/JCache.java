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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import javax.cache.CacheEntry;
import javax.cache.CacheException;
import javax.cache.CacheListener;
import javax.cache.CacheStatistics;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A cache implementation that matches the JCACHE specification.
 * <p/>
 * It is not possible for one class to implement both JCACHE and Ehcache
 * in the same class due to conflicts with method signatures.
 * <p/>
 * This implementation is a decorator for Ehcache, and should exhibit the same
 * characteristics as Ehcache.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class JCache implements javax.cache.Cache {


    /**
     * An Ehcache backing instance
     */
    private Ehcache cache;


    /**
     * A constructor for JCache.
     *
     * JCache is an adaptor for an Ehcache, and therefore requires an Ehcace in its constructor.
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Ehcache} object
     * into {@link net.sf.ehcache.CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param cache An ehcache
     * @since 1.3
     */
    public JCache(Ehcache cache) {
        this.cache = cache;
    }

    /**
     * Add a listener to the list of cache listeners. The behaviour of JCACHE and Ehcache listeners is a little
     * different. See {@link CacheListenerAdaptor} for details on how each event is adapted.
     *
     * @param cacheListener a JCACHE CacheListener
     * @see CacheListenerAdaptor
     */
    public void addListener(CacheListener cacheListener) {
        CacheListenerAdaptor cacheListenerAdaptor = new CacheListenerAdaptor(cacheListener);
        cache.getCacheEventNotificationService().registerListener(cacheListenerAdaptor);
    }

    /**
     * The evict method will remove objects from the cache that are no longer valid.
     * Objects where the specified expiration time has been reached.
     * <p/>
     * This implementation synchronously checks each store for expired elements. Note that the DiskStore
     * has an expiryThread that runs periodically to do the same thing, and that the MemoryStore lazily checks
     * for expiry on overflow and peek, thus reducing the utility of calling this method.
     */
    public void evict() {
        cache.evictExpiredElements();
    }

    /**
     * The getAll method will return, from the cache, a Map of the objects associated with the Collection of keys in argument "keys".
     * If the objects are not in the cache, the associated cache loader will be called. If no loader is associated with an object,
     * a null is returned. If a problem is encountered during the retrieving or loading of the objects, an exception will be thrown.
     * If the "arg" argument is set, the arg object will be passed to the CacheLoader.loadAll method. The cache will not dereference
     * the object. If no "arg" value is provided a null will be passed to the loadAll method. The storing of null values in the cache
     * is permitted, however, the get method will not distinguish returning a null stored in the cache and not finding the object in
     * the cache. In both cases a null is returned.
     *
     * @param collection
     * @return a populated Map of the Cache
     */
    public Map getAll(Collection collection) throws CacheException {

        try {
            List list = cache.getKeysNoDuplicateCheck();
            Map map = new HashMap(list.size());
            for (int i = 0; i < list.size(); i++) {
                Object key = list.get(i);
                Element element = cache.get(key);
                if (element != null) {
                    map.put(key, element.getValue());
                } else {
                    //statement to pass checkstyle
                    map.size();
                    //todo pull through implementation
                }
            }
            return map;
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e.getMessage());
        }
    }

    /**
     * returns the CacheEntry object associated with the key.
     * @param key the key to look for in the cache
     */
    public CacheEntry getCacheEntry(Object key) {
        Element element = cache.get(key);
        if (element != null) {
            return new net.sf.ehcache.jcache.Entry(element);
        } else {
            return null;
        }
    }

    /**
     * Gets an immutable Statistics object representing the Cache statistics at the time. How the statistics are calculated
     * depends on the statistics accuracy setting. The only aspect of statistics sensitive to the accuracy setting is
     * object size. How that is calculated is discussed below.
     * <h3>Best Effort Size</h3>
     * This result is returned when the statistics accuracy setting is {@link CacheStatistics#STATISTICS_ACCURACY_BEST_EFFORT}.
     * <p/>
     * The size is the number of {@link Element}s in the {@link net.sf.ehcache.store.MemoryStore} plus
     * the number of {@link Element}s in the {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This number is the actual number of elements, including expired elements that have
     * not been removed. Any duplicates between stores are accounted for.
     * <p/>
     * Expired elements are removed from the the memory store when
     * getting an expired element, or when attempting to spool an expired element to
     * disk.
     * <p/>
     * Expired elements are removed from the disk store when getting an expired element,
     * or when the expiry thread runs, which is once every five minutes.
     * <p/>
     * <h3>Guaranteed Accuracy Size</h3>
     * This result is returned when the statistics accuracy setting is {@link CacheStatistics#STATISTICS_ACCURACY_GUARANTEED}.
     * <p/>
     * This method accounts for elements which might be expired or duplicated between stores. It take approximately
     * 200ms per 1000 elements to execute.
     * <h3>Fast but non-accurate Size</h3>
     * This result is returned when the statistics accuracy setting is {@link CacheStatistics#STATISTICS_ACCURACY_NONE}.
     * <p/>
     * The number given may contain expired elements. In addition if the DiskStore is used it may contain some double
     * counting of elements. It takes 6ms for 1000 elements to execute. Time to execute is O(log n). 50,000 elements take
     * 36ms.
     *
     * @return the number of elements in the ehcache, with a varying degree of accuracy, depending on accuracy setting.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public CacheStatistics getCacheStatistics() throws IllegalStateException {
        return (javax.cache.CacheStatistics) cache.getStatistics();
    }

    /**
     * todo implement
     * The load method provides a means to "pre load" the cache. This method will, asynchronously, load the specified
     * object into the cache using the associated cacheloader. If the object already exists in the cache, no action is
     *  taken. If no loader is associated with the object, no object will be loaded into the cache. If a problem is
     * encountered during the retrieving or loading of the object, an exception should be logged. If the "arg" argument
     * is set, the arg object will be passed to the CacheLoader.load method. The cache will not dereference the object.
     * If no "arg" value is provided a null will be passed to the load method. The storing of null values in the cache
     * is permitted, however, the get method will not distinguish returning a null stored in the cache and not finding
     * the object in the cache. In both cases a null is returned.
     * @param key key whose associated value to be loaded using the associated cacheloader if this cache doesn't contain it.
     */
    public void load(Object key) throws CacheException {
        try {
            //to pass checkstyle
            int i = 0;
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e.getMessage());
        }
    }

    /**
     * todo implement
     * The loadAll method provides a means to "pre load" objects into the cache. This method will, asynchronously, load
     * the specified objects into the cache using the associated cache loader. If the an object already exists in the
     * cache, no action is taken. If no loader is associated with the object, no object will be loaded into the cache.
     * If a problem is encountered during the retrieving or loading of the objects, an exception (to be defined)
     * should be logged. The getAll method will return, from the cache, a Map of the objects associated with the
     * Collection of keys in argument "keys". If the objects are not in the cache, the associated cache loader will be
     * called. If no loader is associated with an object, a null is returned. If a problem is encountered during the
     * retrieving or loading of the objects, an exception (to be defined) will be thrown. If the "arg" argument is set,
     * the arg object will be passed to the CacheLoader.loadAll method. The cache will not dereference the object.
     * If no "arg" value is provided a null will be passed to the loadAll method.
     *
     * keys - collection of the keys whose associated values to be loaded into this cache by using the associated
     * cacheloader if this cache doesn't contain them.
     */
    public void loadAll(Collection keys) throws CacheException {

        try {
            //to pass checkstyle
            int i = 0;
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e.getMessage());
        }
    }

    /**
     * The peek method will return the object associated with "key" if it currently exists (and is valid) in the cache.
     * If not, a null is returned. With "peek" the CacheLoader will not be invoked and other caches in the system will not be searched.
     *
     * @param key
     * @return the value stored in the cache by key, or null if it does not exist
     */
    public Object peek(Object key) {
        Element element = cache.get(key);
        if (element != null) {
            return element.getValue();
        } else {
            return null;
        }
    }

    /**
     * Remove a listener from the list of cache listeners
     *
     * @param cacheListener a JCACHE CacheListener
     * @see CacheListenerAdaptor
     */
    public void removeListener(CacheListener cacheListener) {
        CacheListenerAdaptor cacheListenerAdaptor = new CacheListenerAdaptor(cacheListener);
        cache.getCacheEventNotificationService().unregisterListener(cacheListenerAdaptor);
    }

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        return cache.getSize();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return (size() == 0);
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this map contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
     * at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map contains a mapping for the specified
     *         key.
     */
    public boolean containsKey(Object key) {
        return cache.isKeyInCache(key);
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.  More formally, returns <tt>true</tt> if and only if
     * this map contains at least one mapping to a value <tt>v</tt> such that
     * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
     * will probably require time linear in the map size for most
     * implementations of the <tt>Map</tt> interface.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    public boolean containsValue(Object value) {
        return cache.isValueInCache(value);
    }

    /**
     * Returns the value to which this map maps the specified key.  Returns
     * <tt>null</tt> if the map contains no mapping for this key.  A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     * <p/>
     * <p>More formally, if this map contains a mapping from a key
     * <tt>k</tt> to a value <tt>v</tt> such that <tt>(key==null ? k==null :
     * key.equals(k))</tt>, then this method returns <tt>v</tt>; otherwise
     * it returns <tt>null</tt>.  (There can be at most one such mapping.)
     *
     * @param key key whose associated value is to be returned.
     * @return the value to which this map maps the specified key, or
     *         <tt>null</tt> if the map contains no mapping for this key.
     * @see #containsKey(Object)
     */
    public Object get(Object key) {
        return cache.get(key);
    }

    /**
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * this key, the old value is replaced by the specified value.  (A map
     * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
     * if {@link #containsKey(Object) m.containsKey(k)} would return
     * <tt>true</tt>.))
     *
     * @param key   key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.  A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key, if the implementation supports
     *         <tt>null</tt> values.
     */
    public Object put(Object key, Object value) {
        Element element = cache.get(key);
        cache.put(new Element(key, value));
        if (element != null) {
            return element.getValue();
        } else {
            return null;
        }
    }

    /**
     * Removes the mapping for this key from this map if it is present
     * (optional operation).   More formally, if this map contains a mapping
     * from key <tt>k</tt> to value <tt>v</tt> such that
     * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
     * is removed.  (The map can contain at most one such mapping.)
     * <p/>
     * <p>Returns the value to which the map previously associated the key, or
     * <tt>null</tt> if the map contained no mapping for this key.  (A
     * <tt>null</tt> return can also indicate that the map previously
     * associated <tt>null</tt> with the specified key if the implementation
     * supports <tt>null</tt> values.)  The map will not contain a mapping for
     * the specified  key once the call returns.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key.
     */
    public Object remove(Object key) {
        Element element = cache.get(key);
        cache.remove(key);
        if (element != null) {
            return element.getValue();
        } else {
            return null;
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map
     * (optional operation).  The effect of this call is equivalent to that
     * of calling {@link #put(Object,Object) put(k, v)} on this map once
     * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
     * specified map.  The behavior of this operation is unspecified if the
     * specified map is modified while the operation is in progress.
     *
     * @param sourceMap Mappings to be stored in this map.
     */

    public void putAll(Map sourceMap) {
        for (Iterator iterator = sourceMap.keySet().iterator(); iterator.hasNext();) {
            Object key = iterator.next();
            cache.put(new Element(key, sourceMap.get(key)));
        }
    }

    /**
     * Removes all mappings from this map (optional operation).
     */
    public void clear() {
        cache.removeAll();
    }

    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  If the map is modified while an iteration over the set is
     * in progress (except through the iterator's own <tt>remove</tt>
     * operation), the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding mapping from
     * the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt> <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the add or <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set keySet() {
        List list = cache.getKeysNoDuplicateCheck();
        Set set = new HashSet();
        set.addAll(list);
        return set;
    }

    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  If the map is modified while an
     * iteration over the collection is in progress (except through the
     * iterator's own <tt>remove</tt> operation), the results of the
     * iteration are undefined.  The collection supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations.
     * It does not support the add or <tt>addAll</tt> operations.
     * <p/>
     * Contradicting the above Map contract, whether or not changes to an entry affect the entry in the cache is undefined.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection values() {
        List list = cache.getKeysNoDuplicateCheck();
        Set set = new HashSet(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object key = list.get(i);
            Element element = cache.get(key);
            if (element != null) {
                set.add(element.getValue());
            }
        }
        return set;
    }

    /**
     * Returns a set view of the mappings contained in this map.  Each element
     * in the returned set is a {@link java.util.Map.Entry}.  The set is backed by the
     * map, so changes to the map are reflected in the set, and vice-versa.
     * If the map is modified while an iteration over the set is in progress
     * (except through the iterator's own <tt>remove</tt> operation, or through
     * the <tt>setValue</tt> operation on a map entry returned by the iterator)
     * the results of the iteration are undefined.  The set supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not support
     * the <tt>add</tt> or <tt>addAll</tt> operations.
     * <p/>
     * Contradicting the above Map contract, whether or not changes to an entry affect the entry in the cache is undefined.
     *
     * @return a set view of the mappings contained in this map.
     */
    public Set entrySet() {
        List list = cache.getKeysNoDuplicateCheck();
        Set set = new HashSet(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object key = list.get(i);
            Element element = cache.get(key);
            if (element != null) {
                set.add(new net.sf.ehcache.jcache.Entry(element));
            }
        }
        return set;
    }

    /**
     * Sets the statistics accuracy.
     *
     * @param statisticsAccuracy one of {@link CacheStatistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link CacheStatistics#STATISTICS_ACCURACY_GUARANTEED}, {@link CacheStatistics#STATISTICS_ACCURACY_NONE}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        cache.setStatisticsAccuracy(statisticsAccuracy);
    }


}
