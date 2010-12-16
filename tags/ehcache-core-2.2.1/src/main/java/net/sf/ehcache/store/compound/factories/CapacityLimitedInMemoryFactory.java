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

package net.sf.ehcache.store.compound.factories;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import net.sf.ehcache.Element;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.Policy;

import net.sf.ehcache.store.compound.ElementSubstitute;
import net.sf.ehcache.store.compound.ElementSubstituteFilter;
import net.sf.ehcache.store.compound.IdentityElementSubstituteFactory;
import net.sf.ehcache.store.compound.CompoundStore;

/**
 * An implementation of a capacity limited in-memory factory.
 * 
 * @author Chris Dennis
 */
public class CapacityLimitedInMemoryFactory implements IdentityElementSubstituteFactory {

    private static final int MAX_EVICT = 5;
    private static final int SAMPLE_SIZE = 30;
    
    private final AtomicInteger count = new AtomicInteger();
    private final DiskOverflowStorageFactory secondary;
    private final RegisteredEventListeners eventService;
    private final ElementSubstituteFilter<Element> filter = new ElementSubstituteFilter<Element>() {
        public boolean allows(Object object) {
            return created(object);
        }
    };
    
    private volatile CompoundStore boundStore;
    private volatile int capacity;
    private volatile Policy policy;
        
    /**
     * Constructs a factory with the given secondary (null if none), capacity, and eviction policy.
     * 
     * @param secondary factory to evict through
     * @param capacity maximum capacity
     * @param policy policy to use on eviction
     */
    public CapacityLimitedInMemoryFactory(DiskOverflowStorageFactory secondary, int capacity,
            Policy policy, RegisteredEventListeners eventService) {
        this.secondary = secondary;
        if (secondary != null) {
            this.secondary.primary(this);
        }
        this.capacity = capacity;
        this.policy = policy;
        this.eventService = eventService;
    }

    /**
     * {@inheritDoc}
     */
    public void bind(CompoundStore store) {
        boundStore = store;
        if (secondary != null) {
            secondary.bind(store);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void unbind(CompoundStore store) {
        if (secondary != null) {
            secondary.unbind(store);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Element create(Object key, Element element) {
        int size = count.incrementAndGet();
        if (capacity > 0) {
            int overflow = size - capacity;
            if (overflow > 0) {
                evict(Math.min(MAX_EVICT, overflow), key, size);
            }
        }
        
        return element;
    }
    
    private void evict(int n, Object keyHint, int size) {
        for (int i = 0; i < n; i++) {
            Element target = getEvictionTarget(keyHint, size);
            if (target == null) {
                continue;
            }
            if (target.isExpired()) {
                if (boundStore.evict(target.getObjectKey(), target)) {
                    eventService.notifyElementExpiry(target, false);
                }
            } else if (secondary == null) {
                if (boundStore.evict(target.getObjectKey(), target)) {
                    eventService.notifyElementEvicted(target, false);
                }
            } else {
                try {
                    ElementSubstitute substitute = secondary.create(target.getObjectKey(), target);
                    boundStore.fault(target.getObjectKey(), target, substitute);
                } catch (IllegalArgumentException e) {
                    if (boundStore.evict(target.getObjectKey(), target)) {
                        eventService.notifyElementEvicted(target, false);
                    }
                }
            }
        }
    }

    private Element getEvictionTarget(Object keyHint, int size) {
        List<Element> sample = boundStore.getRandomSample(filter, Math.min(SAMPLE_SIZE, size), keyHint);
        return policy.selectedBasedOnPolicy(sample.toArray(new Element[sample.size()]), null);
    }
    
    /**
     * {@inheritDoc}
     */
    public Element retrieve(Object key, Element object) {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public void free(Lock exclusion, Element object) {
        count.decrementAndGet();
    }

    /**
     * Get the count of elements created by this factory
     */
    public int getSize() {
        return count.get();
    }
    
    /**
     * Get the total serialized size of all elements created by this factory
     */
    public long getSizeInBytes() {
        long size = 0;
        for (Object o : boundStore.getKeyArray()) {
            Object e = boundStore.unretrievedGet(o);
            if (created(e)) {
                size += ((Element) e).getSerializedSize();
            }
        }
        return size;
    }
    
    /**
     * Return the eviction policy used by this factory.
     */
    public Policy getEvictionPolicy() {
        return policy;
    }
    
    /**
     * Set the eviction policy used by this factory.
     */
    public void setEvictionPolicy(Policy policy) {
        this.policy = policy;
    }

    /**
     * Set the maximum capacity of this factory.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * {@inheritDoc}
     */
    public boolean created(Object object) {
        return object instanceof Element;
    }

    /**
     * Remove elements created by this factory if they have expired.
     */
    public void expireElements() {
        for (Object key : boundStore.keySet()) {
            Object value = boundStore.unretrievedGet(key);
            if (value instanceof Element) {
                Element e = (Element) value;
                if (e.isExpired() && boundStore.evict(key, value)) {
                    eventService.notifyElementExpiry(e, false);
                }
            }
        }
    }    
}
