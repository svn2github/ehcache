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

package net.sf.ehcache.store.compound.factories;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Policy;

import net.sf.ehcache.store.compound.ElementSubstitute;
import net.sf.ehcache.store.compound.IdentityElementSubstituteFactory;
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.factories.DiskOverflowStorageFactory.Placeholder;

public class CapacityLimitedInMemoryFactory implements IdentityElementSubstituteFactory {

    private static final int MAX_EVICT = 5;
    private static final int SAMPLE_SIZE = 30;
    
    private final AtomicInteger count = new AtomicInteger();
    private final DiskOverflowStorageFactory secondary;
    
    private volatile CompoundStore boundStore;
    private volatile int capacity;
    private volatile Policy policy;
        
    public CapacityLimitedInMemoryFactory(DiskOverflowStorageFactory secondary, int capacity, Policy policy) {
        this.secondary = secondary;
        if (secondary != null) {
            this.secondary.primary(this);
        }
        this.capacity = capacity;
        this.policy = policy;
    }

    public void bind(CompoundStore store) {
        boundStore = store;
        if (secondary != null) {
            secondary.bind(store);
        }
    }
    
    public void unbind(CompoundStore store) {
        if (secondary != null) {
            secondary.unbind(store);
        }
    }
    
    public Element create(Object key, Element element) {
        int size = count.incrementAndGet();
        if (capacity > 0) {
            int overflow = size - capacity;
            if (overflow > 0) {
                evict(Math.min(MAX_EVICT, overflow), key);
            }
        }
        
        return element;
    }
    
    private void evict(int n, Object keyHint) {
        for (int i = 0; i < n; i++) {
            List<Element> sample = boundStore.getRandomSample(this, SAMPLE_SIZE, keyHint);
            Element target = policy.selectedBasedOnPolicy(sample.toArray(new Element[sample.size()]), null);
            if (target == null) {
//                System.err.println("Eviction Selected null Target");
//                System.err.println(sample);
                continue;
            }
            if (secondary == null) {
                boundStore.evict(target.getObjectKey(), target);
            } else {
                try {
                    ElementSubstitute substitute = secondary.create(target.getObjectKey(), target);
                    if (boundStore.fault(target.getObjectKey(), target, substitute)) {
                        ((Placeholder) substitute).schedule();
                    }
                } catch (IllegalArgumentException e) {
                    boundStore.evict(target.getObjectKey(), target);
                }
            }
        }
    }

    public Element retrieve(Object key, Element object) {
        return object;
    }

    public void free(Element object) {
        count.decrementAndGet();
    }

    public int getSize() {
        return count.get();
    }
    
    public long getSizeInBytes() {
        long size = 0;
        for (Object o : boundStore.getKeyArray()) {
            Object e = boundStore.unretrievedGet(o);
            if (e instanceof Element) {
                size += ((Element) e).getSerializedSize();
            }
        }
        return size;
    }
    
    public Policy getEvictionPolicy() {
        return policy;
    }
    
    public void setEvictionPolicy(Policy policy) {
        this.policy = policy;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean created(Object object) {
        return object instanceof Element;
    }    
}
