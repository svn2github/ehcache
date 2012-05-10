/**
 *  Copyright Terracotta, Inc.
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
package net.sf.ehcache.hibernate.strategy;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.hibernate.regions.EhcacheCollectionRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * JTA CollectionRegionAccessStrategy.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 */
public class TransactionalEhcacheCollectionRegionAccessStrategy extends AbstractEhcacheAccessStrategy<EhcacheCollectionRegion>
        implements CollectionRegionAccessStrategy {

    private final Ehcache ehcache;

    /**
     * Construct a new collection region access strategy.
     * @param region the Hibernate region.
     * @param ehcache the cache.
     * @param settings the Hibernate settings.
     */
    public TransactionalEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region, Ehcache ehcache, Settings settings) {
        super(region, settings);
        this.ehcache = ehcache;
    }



    /**
     * {@inheritDoc}
     */
    public Object get(Object key, long txTimestamp) throws CacheException {
        try {
            Element element = ehcache.get(key);
            return element == null ? null : element.getObjectValue();
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CollectionRegion getRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     */
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean putFromLoad(Object key, Object value, long txTimestamp,
                               Object version, boolean minimalPutOverride) throws CacheException {
        try {
            if (minimalPutOverride && ehcache.get(key) != null) {
                return false;
            }
            //OptimisticCache? versioning?
            ehcache.put(new Element(key, value));
            return true;
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Object key) throws CacheException {
        try {
            ehcache.remove(key);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        // no-op
    }

}
