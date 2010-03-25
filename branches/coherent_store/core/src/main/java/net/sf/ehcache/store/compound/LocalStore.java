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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.CacheWriterManager;

public abstract class LocalStore implements Store {

    private static final int MAXIMUM_CAPACITY = 1 << 30; 
    private static final int RETRIES_BEFORE_LOCK = 2;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int DEFAULT_SEGMENT_COUNT = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    private final InternalElementSubstituteFactory<?> primary;
    private final Random rndm = new Random();
    private final Segment[] segments;
    private final int segmentShift;
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.STATUS_UNINITIALISED);

    private volatile CacheLockProvider lockProvider;

    public LocalStore(InternalElementSubstituteFactory<?> primary) {
        this(primary, (primary instanceof IdentityElementSubstituteFactory) ? (IdentityElementSubstituteFactory) primary : null);
    }
    
    public LocalStore(InternalElementSubstituteFactory<?> primary, IdentityElementSubstituteFactory identity) {
        this.segments = new Segment[DEFAULT_SEGMENT_COUNT];
        this.segmentShift = Integer.numberOfLeadingZeros(segments.length - 1);

        for (int i = 0; i < this.segments.length; ++i)
            this.segments[i] = new Segment(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, primary, identity);
        
        this.primary = primary;
        primary.bind(this);
        status.set(Status.STATUS_ALIVE);
    }

    public boolean put(Element element) {
        Object key = element.getObjectKey();
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, element, false) == null;
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) {
        boolean result = put(element);
        if (writerManager != null) {
            writerManager.put(element);
        }
        return result;
    }

    public Element get(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).get(key, hash);
    }

    public Element getQuiet(Object key) {
        return get(key);
    }

    public Object unretrievedGet(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).unretrievedGet(key, hash);
    }
    
    public Object[] getKeyArray() {
        return new KeySet().toArray();
    }
    
    public Element remove(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).remove(key, hash, null);
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) {
        Element removed = remove(key);
        if (writerManager != null) {
            writerManager.remove(new CacheEntry(key, removed));
        }
        return removed;
    }

    public void removeAll() {
        for (Segment s : segments) {
            s.clear();
        }
    }
    
    public void dispose() {
        if (status.compareAndSet(Status.STATUS_ALIVE, Status.STATUS_SHUTDOWN)) {
            primary.unbind(this);
        }
    }
    
    public int getSize() {
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

    public Status getStatus() {
        return status.get();
    }
    
    public boolean containsKey(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).containsKey(key, hash);
    }

    public Object getInternalContext() {
        CacheLockProvider context = lockProvider;
        return (lockProvider != null) ? lockProvider : (lockProvider = new LockProvider(this));
    }
    
    public boolean isCacheCoherent() {
        return false;
    }
    
    public boolean isClusterCoherent() {
        return false;
    }
    
    public boolean isNodeCoherent() {
        return false;
    }
    
    public void setNodeCoherent(boolean coherent) {
        throw new UnsupportedOperationException();
    }
    
    public void waitUntilClusterCoherent() {
        throw new UnsupportedOperationException();
    }
    
    public Element putIfAbsent(Element element) {
        Object key = element.getObjectKey();
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, element, true);
    }
    
    public boolean fault(Object key, Object expect, Object fault) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).fault(key, hash, expect, fault);
    }
    
    public boolean exclusiveFault(Object key, Object expect, Object fault) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).exclusiveFault(key, hash, expect, fault);
    }
    
    public boolean evict(Object key, Element element) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).evict(key, hash, element);
    }
    
    public <T> List<T> getRandomSample(InternalElementSubstituteFactory<T> factory, int sampleSize, Object keyHint) {
        ArrayList<T> sampled = new ArrayList<T>(sampleSize);
        
        // pick a random starting point in the map
        int randomHash = rndm.nextInt();

        final int segmentStart = (hash(keyHint.hashCode()) >>> segmentShift);
        int segmentIndex = segmentStart;
        do {
            segments[segmentIndex].addRandomSample(factory, sampleSize, sampled, randomHash);
            if (sampled.size() >= sampleSize) {
                break;
            }
            //move to next segment
            segmentIndex = (segmentIndex + 1) & (segments.length - 1);
        } while (segmentIndex != segmentStart);

        return sampled;
    }
    
    private static int hash(int hash) {
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

    final class KeySet extends AbstractSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return LocalStore.this.getSize();
        }
        
        @Override
        public boolean contains(Object o) {
            return LocalStore.this.containsKey(o);
        }
        
        @Override
        public boolean remove(Object o) {
            return LocalStore.this.remove(o) != null;
        }
        
        @Override
        public void clear() {
            LocalStore.this.removeAll();
        }
        
        @Override
        public Object[] toArray() {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this)
                c.add(object);
            return c.toArray();
        }
        
        @Override
        public <T> T[] toArray(T[] a) {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this)
                c.add(object);
            return c.toArray(a);
        }
    }

    static class LockProvider implements CacheLockProvider {

        private final LocalStore store;
        
        LockProvider(LocalStore store) {
            this.store = store;
        }
        
        public Sync[] getAndWriteLockAllSyncForKeys(Object... keys) {
            Set<Segment> segments = getSegmentsFor(keys);
            
            List<Sync> ordered = new ArrayList<Sync>();
            for (Segment s : store.segments) {
                if (segments.contains(s)) {
                    s.writeLock().lock();
                    ordered.add(new ReadWriteLockSync(s));
                }
            }
            
            return ordered.toArray(new Sync[ordered.size()]);
        }

        public Sync getSyncForKey(Object key) {
            int hash = store.hash(key.hashCode());
            return new ReadWriteLockSync(store.segmentFor(hash));
        }

        public void unlockWriteLockForAllKeys(Object... keys) {
            for (Segment s : getSegmentsFor(keys)) {
                s.writeLock().unlock();
            }
        }
        
        private Set<Segment> getSegmentsFor(Object... keys) {
            Set<Segment> segments = new HashSet<Segment>();
            
            for (Object k : keys) {
                segments.add(store.segmentFor(hash(k.hashCode())));
            }
            
            return segments;
        }
    }
    
    abstract class HashIterator {
        private int nextSegmentIndex;
        private Iterator<HashEntry> currentIterator;

        public HashIterator() {
            nextSegmentIndex = segments.length;
            
            while (nextSegmentIndex > 0) {
                nextSegmentIndex--;
                currentIterator = segments[nextSegmentIndex].hashIterator();
                if (currentIterator.hasNext()) {
                    return;
                }
            }
        }

        public boolean hasNext() {
            if (this.currentIterator == null)
                return false;

            if (this.currentIterator.hasNext()) {
                return true;
            } else {
                while (nextSegmentIndex > 0) {
                    nextSegmentIndex--;
                    currentIterator = segments[nextSegmentIndex].hashIterator();
                    if (currentIterator.hasNext()) {
                        return true;
                    }
                }
            }
            return false;
        }

        public HashEntry nextEntry() {
            HashEntry item = null;

            if (currentIterator == null)
                return null;

            if (currentIterator.hasNext()) {
                return currentIterator.next();
            } else {
                while (nextSegmentIndex > 0) {
                    nextSegmentIndex--;
                    currentIterator = segments[nextSegmentIndex].hashIterator();
                    if (currentIterator.hasNext()) {
                        return currentIterator.next();
                    }
                }
            }
            return null;
        }

        public void remove() {
            currentIterator.remove();
        }
    }

    final class KeyIterator extends HashIterator implements Iterator<Object> {
        public Object next() {
            return super.nextEntry().key;
        }
    }
    
    static class ReadWriteLockSync implements Sync {

        private final ReadWriteLock lock;
        
        ReadWriteLockSync(ReadWriteLock lock) {
            this.lock = lock;
        }
        
        public void lock(LockType type) {
            switch (type) {
            case READ:
                lock.readLock().lock();
                break;
            case WRITE:
                lock.writeLock().lock();
                break;
            }
        }

        public boolean tryLock(LockType type, long msec) throws InterruptedException {
            switch (type) {
                case READ:
                    return lock.readLock().tryLock(msec, TimeUnit.MILLISECONDS);
                case WRITE:
                    return lock.writeLock().tryLock(msec, TimeUnit.MILLISECONDS);
            }
            return false;
        }

        public void unlock(LockType type) {
            switch (type) {
                case READ:
                    lock.readLock().unlock();
                    break;
                case WRITE:
                    lock.writeLock().unlock();
                    break;
            }
        }
        
    }
}