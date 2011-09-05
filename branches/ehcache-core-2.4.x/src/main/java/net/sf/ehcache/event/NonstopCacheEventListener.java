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

package net.sf.ehcache.event;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link CacheEventListener} implementation that uses nonstop feature
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopCacheEventListener implements CacheEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonstopCacheEventListener.class);

    private final NonstopStore nonstopStore;

    private final CacheEventListener underlyingListener;

    /**
     * Public constructor
     *
     * @param nonstopActiveDelegateHolder
     * @param underlyingListener
     */
    public NonstopCacheEventListener(NonstopActiveDelegateHolder nonstopActiveDelegateHolder, CacheEventListener underlyingListener) {
        this.underlyingListener = underlyingListener;
        this.nonstopStore = nonstopActiveDelegateHolder.getNonstopStore();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        this.nonstopStore.executeClusterOperation(new CacheEventClusteredOperation(cache, CacheEventType.REMOVED, underlyingListener,
                element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        this.nonstopStore.executeClusterOperation(new CacheEventClusteredOperation(cache, CacheEventType.PUT, underlyingListener, element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        this.nonstopStore.executeClusterOperation(new CacheEventClusteredOperation(cache, CacheEventType.UPDATED, underlyingListener,
                element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        this.nonstopStore.executeClusterOperation(new CacheEventClusteredOperation(cache, CacheEventType.EXPIRED, underlyingListener,
                element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        this.nonstopStore.executeClusterOperation(new CacheEventClusteredOperation(cache, CacheEventType.EVICTED, underlyingListener,
                element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(Ehcache cache) {
        this.nonstopStore.executeClusterOperation(new CacheEventClusteredOperation(cache, CacheEventType.REMOVE_ALL, underlyingListener,
                null));
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        this.nonstopStore.executeClusterOperation(new CacheEventClusteredOperation(null, CacheEventType.DISPOSE, underlyingListener, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Private enum to distinguish various event types
     *
     * @author Abhishek Sanoujam
     *
     */
    private static enum CacheEventType {
        PUT, REMOVED, UPDATED, EXPIRED, EVICTED, REMOVE_ALL, DISPOSE
    }

    /**
     * Private class
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class CacheEventClusteredOperation implements ClusterOperation<Void> {

        private final CacheEventType eventType;
        private final CacheEventListener underlyingListener;
        private final Ehcache cache;
        private final Element element;

        public CacheEventClusteredOperation(Ehcache cache, CacheEventType eventType, CacheEventListener underlyingListener, Element element) {
            this.cache = cache;
            this.eventType = eventType;
            this.underlyingListener = underlyingListener;
            this.element = element;
        }

        public Void performClusterOperation() throws Exception {
            switch (eventType) {
                case PUT:
                    this.underlyingListener.notifyElementPut(cache, element);
                    break;
                case REMOVED:
                    this.underlyingListener.notifyElementRemoved(cache, element);
                    break;
                case UPDATED:
                    this.underlyingListener.notifyElementUpdated(cache, element);
                    break;
                case EXPIRED:
                    this.underlyingListener.notifyElementExpired(cache, element);
                    break;
                case EVICTED:
                    this.underlyingListener.notifyElementEvicted(cache, element);
                    break;
                case REMOVE_ALL:
                    this.underlyingListener.notifyRemoveAll(cache);
                    break;
                case DISPOSE:
                    this.underlyingListener.dispose();
                    break;
                default:
                    throw new CacheException("Unknown type of cache event notification: " + eventType);
            }
            return null;
        }

        public Void performClusterOperationTimedOut(TimeoutBehaviorType configuredTimeoutBehavior) {
            LOGGER.info("Terracotta clustered event notification timed out: operation: " + eventType + ", cache: " + cache.getName()
                    + ", element: " + element);
            return null;
        }

    }

}
