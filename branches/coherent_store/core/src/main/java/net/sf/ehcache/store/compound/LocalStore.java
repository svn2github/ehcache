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

package net.sf.ehcache.store.compound;

import java.util.Set;

import net.sf.ehcache.Element;

public class LocalStore {

    static final int MAXIMUM_CAPACITY = 1 << 30; 
    static final int RETRIES_BEFORE_LOCK = 2;
    
    final Segment[] segments;
    final int segmentShift;
    
    LocalStore(InternalElementProxyFactory primary, Set<InternalElementProxyFactory> factories) {
        this.segments = new Segment[16];
        this.segmentShift = Integer.numberOfLeadingZeros(segments.length - 1);

        for (int i = 0; i < this.segments.length; ++i)
            this.segments[i] = new Segment(16, 0.75f, primary, factories);
    }

    public Element get(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).get(key, hash);
    }

    public Element put(Element value) {
        Object key = value.getObjectKey();
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, value, false);
    }

    public Element putIfAbsent(Element value) {
        Object key = value.getObjectKey();
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, value, true);
    }

    public Element remove(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).remove(key, hash, null);
    }

    public int size() {
        final Segment[] segments = this.segments;
        long sum = 0;
        long check = 0;
        int[] mc = new int[segments.length];
        // Try a few times to get accurate count. On failure due to
        // continuous async changes in table, resort to locking.
        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            check = 0;
            sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                sum += segments[i].count;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    check += segments[i].count;
                    if (mc[i] != segments[i].modCount) {
                        check = -1; // force retry
                        break;
                    }
                }
            }
            if (check == sum) 
                break;
        }
        if (check != sum) { // Resort to locking all segments
            sum = 0;
            for (int i = 0; i < segments.length; ++i) 
                segments[i].readLock().lock();
            for (int i = 0; i < segments.length; ++i) 
                sum += segments[i].count;
            for (int i = 0; i < segments.length; ++i) 
                segments[i].readLock().unlock();
        }
        if (sum > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else
            return (int)sum;
    }
    
    public int size(InternalElementProxyFactory factory) {
        long sum = 0;
        for (int i = 0; i < segments.length; ++i) {
            sum += segments[i].size(factory);
        }
        if (sum > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else
            return (int)sum;
    }
    
    public boolean fault(Object key, Object expect, Object fault) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).fault(key, hash, expect, fault);
    }
    
    private int hash(int hash) {
        hash += (hash << 15 ^ 0xFFFFCD7D);
        hash ^= hash >>> 10;
        hash += (hash << 3);
        hash ^= hash >>> 6;
        hash += (hash << 2) + (hash << 14);
        return (hash ^ hash >>> 16);
    }
    
    private Segment segmentFor(int hash) {
        return segments[hash >>> segmentShift];
    }
}
