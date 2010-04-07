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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * Abstract compound store implementation.
 * 
 * @author Chris Dennis
 */
public abstract class CompoundStore implements Store {

    /**
     * Checkstyle is crap - this is pointless
     */
    private static final int FFFFCD7D = 0xffffcd7d;
    private static final int FIFTEEN = 15;
    private static final int TEN = 10;
    private static final int THREE = 3;
    private static final int SIX = 6;
    private static final int FOURTEEN = 14;
    private static final int SIXTEEN = 16;
    
    private static final int MAXIMUM_CAPACITY = Integer.highestOneBit(Integer.MAX_VALUE); 
    private static final int RETRIES_BEFORE_LOCK = 2;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int DEFAULT_SEGMENT_COUNT = 64;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    private final InternalElementSubstituteFactory<?> primary;
    private final Random rndm = new Random();
    private final Segment[] segments;
    private final int segmentShift;
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.STATUS_UNINITIALISED);

    private volatile CacheLockProvider lockProvider;

    private volatile Set<Object> keySet;
    
    /**
     * Create a CompoundStore using the supplied factory as the primary factory.
     * 
     * @param primary factory which new elements are passed through
     */
    public CompoundStore(InternalElementSubstituteFactory<?> primary) {
        this(primary, (primary instanceof IdentityElementSubstituteFactory) ? (IdentityElementSubstituteFactory) primary : null);
    }

    /**
     * Create a CompoundStore using the supplied primary, and designated identity factory.
     * 
     * @param primary factory which new elements are passed through
     * @param identity factory which performs identity substitution
     */
    public CompoundStore(InternalElementSubstituteFactory<?> primary, IdentityElementSubstituteFactory identity) {
        this(primary, identity, false, false, null);
    }
    /**
     * Create a CompoundStore using the supplied primary, and designated identity factory.
     *
     * @param primary factory which new elements are passed through
     * @param identity factory which performs identity substitution
     * @param copyOnRead true should we copy Elements on reads, otherwise false
     * @param copyOnWrite true should we copy Elements on writes, otherwise false
     * @param copyStrategy the strategy to copy elements (needs to be non null if copyOnRead or copyOnWrite is true)
     */
    public CompoundStore(InternalElementSubstituteFactory<?> primary, IdentityElementSubstituteFactory identity,
                         boolean copyOnRead, boolean copyOnWrite, CopyStrategy copyStrategy) {
        this.segments = new Segment[DEFAULT_SEGMENT_COUNT];
        this.segmentShift = Integer.numberOfLeadingZeros(segments.length - 1);

        for (int i = 0; i < this.segments.length; ++i) {
            this.segments[i] = new Segment(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, primary, identity,
                copyOnRead, copyOnWrite, copyStrategy);
        }
        
        this.primary = primary;
        primary.bind(this);
        status.set(Status.STATUS_ALIVE);
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) {
        if (element == null) {
            return false;
        } else {
            Object key = element.getObjectKey();
            int hash = hash(key.hashCode());
            return segmentFor(hash).put(key, hash, element, false) == null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) {
        boolean result = put(element);
        if (writerManager != null) {
            writerManager.put(element);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        if (key == null) {
            return null;
        }
        
        int hash = hash(key.hashCode());
        return segmentFor(hash).get(key, hash);
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        return get(key);
    }

    /**
     * Return the unretrieved (undecoded) value for this key
     * 
     * @param key key to lookup
     * @return Element or ElementSubstitute
     */
    public Object unretrievedGet(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).unretrievedGet(key, hash);
    }
    
    /**
     * Put the given encoded element directly into the store
     */
    public boolean putRawIfAbsent(Object key, Object encoded) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).putRawIfAbsent(key, hash, encoded);
    }
    
    /**
     * {@inheritDoc}
     */
    public Object[] getKeyArray() {
        return keySet().toArray();
    }

    /**
     * Get a set view of the keys in this store
     */
    public Set<Object> keySet() {
        if (keySet != null) {
            return keySet;
        } else {
            keySet = new KeySet();
            return keySet;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        if (key == null) {
            return null;
        }
        
        int hash = hash(key.hashCode());
        return segmentFor(hash).remove(key, hash, null);
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) {
        Element removed = remove(key);
        if (writerManager != null) {
            writerManager.remove(new CacheEntry(key, removed));
        }
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        for (Segment s : segments) {
            s.clear();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void dispose() {
        if (status.compareAndSet(Status.STATUS_ALIVE, Status.STATUS_SHUTDOWN)) {
            primary.unbind(this);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public int getSize() {
        final Segment[] segs = this.segments;
        long size = -1;
        // Try a few times to get accurate count. On failure due to
        // continuous async changes in table, resort to locking.
        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            size = volatileSize(segs);
            if (size >= 0) {
                break;
            }
        }
        if (size < 0) {
            // Resort to locking all segments
            size = lockedSize(segs);
        }
        if (size > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int)size;
        }
    }

    private static long volatileSize(Segment[] segs) {
        int[] mc = new int[segs.length];
        long check = 0;
        long sum = 0;
        int mcsum = 0;
        for (int i = 0; i < segs.length; ++i) {
            sum += segs[i].count;
            mc[i] = segs[i].modCount;
            mcsum += mc[i];
        }
        if (mcsum != 0) {
            for (int i = 0; i < segs.length; ++i) {
                check += segs[i].count;
                if (mc[i] != segs[i].modCount) {
                    return -1;
                }
            }
        }
        if (check == sum) {
            return sum;
        } else {
            return -1;
        }
    }
    
    private static long lockedSize(Segment[] segs) {
        long size = 0;
        for (int i = 0; i < segs.length; ++i) { 
            segs[i].readLock().lock();
        }
        for (int i = 0; i < segs.length; ++i) { 
            size += segs[i].count;
        }
        for (int i = 0; i < segs.length; ++i) {
            segs[i].readLock().unlock();
        }
        
        return size;
    }
    
    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return status.get();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).containsKey(key, hash);
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        if (lockProvider != null) {
            return lockProvider;
        } else {
            lockProvider = new LockProvider();
            return lockProvider;
        }
    }
    
    /**
     * Not used as this store is not clustered
     * 
     * @return <code>false</code>
     */
    public boolean isCacheCoherent() {
        return false;
    }
    
    /**
     * Not used as this store is not clustered
     * 
     * @return <code>false</code>
     */
    public boolean isClusterCoherent() {
        return false;
    }
    
    /**
     * Not used as this store is not clustered
     * 
     * @return <code>false</code>
     */
    public boolean isNodeCoherent() {
        return false;
    }
    
    /**
     * Not used as this store is not clustered
     * 
     * @throws UnsupportedOperationException always
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Not used as this store is not clustered
     * 
     * @throws UnsupportedOperationException always
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * New putIfAbsent method for store - this needs to be exposed on the cache too...
     */
    public Element putIfAbsent(Element element) {
        Object key = element.getObjectKey();
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, element, true);
    }
    
    /**
     * Atomically switch (CAS) the <code>expect</code> representation of this element for the
     * <code>fault</code> representation.
     * <p>
     * A successful switch will return <code>true</code>, and free the replaced element/element-proxy.
     * A failed switch will return <code>false</code> and free the element/element-proxy which was not
     * installed.
     * 
     * @param key key to which this element (proxy) is mapped
     * @param expect element (proxy) expected
     * @param fault element (proxy) to install
     * @return <code>true</code> if <code>fault</code> was installed
     */
    public boolean fault(Object key, Object expect, Object fault) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).fault(key, hash, expect, fault);
    }
    
    /**
     * Remove the matching mapping.  Unlike the {@link Segment#remove(Object, int, Object)} method 
     * evict does referential comparison of the unretrieved substitute against the argument value.
     * 
     * @param key key to match against
     * @param substitute optional value to match against
     * @return <code>true</code> on a successful remove
     */
    public boolean evict(Object key, Object substitute) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).evict(key, hash, substitute);
    }
    
    /**
     * Select a random sample of elements generated by the supplied factory.
     * 
     * @param <T> type of the elements or element substitutes
     * @param factory generator of the given type
     * @param sampleSize minimum number of elements to return
     * @param keyHint a key on which we are currently working
     * @return list of sampled elements/element substitute
     */
    public <T> List<T> getRandomSample(ElementSubstituteFilter<T> factory, int sampleSize, Object keyHint) {
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
        int spread = hash;
        spread += (spread << FIFTEEN ^ FFFFCD7D);
        spread ^= spread >>> TEN;
        spread += (spread << THREE);
        spread ^= spread >>> SIX;
        spread += (spread << 2) + (spread << FOURTEEN);
        return (spread ^ spread >>> SIXTEEN);
    }
    
    private Segment segmentFor(int hash) {
        return segments[hash >>> segmentShift];
    }

    /**
     * Key set implementation for the CompoundStore
     */
    final class KeySet extends AbstractSet<Object> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<Object> iterator() {
            return new KeyIterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return CompoundStore.this.getSize();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object o) {
            return CompoundStore.this.containsKey(o);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean remove(Object o) {
            return CompoundStore.this.remove(o) != null;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void clear() {
            CompoundStore.this.removeAll();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public Object[] toArray() {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this) {
                c.add(object);
            }
            return c.toArray();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T[] toArray(T[] a) {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this) {
                c.add(object);
            }
            return c.toArray(a);
        }
    }

    /**
     * LockProvider implementation that uses the segment locks. 
     */
    private class LockProvider implements CacheLockProvider {

        /**
         * {@inheritDoc}
         */
        public Sync[] getAndWriteLockAllSyncForKeys(Object... keys) {
            Set<Segment> segs = getSegmentsFor(keys);
            
            List<Sync> ordered = new ArrayList<Sync>();
            for (Segment s : CompoundStore.this.segments) {
                if (segs.contains(s)) {
                    s.writeLock().lock();
                    ordered.add(new ReadWriteLockSync(s));
                }
            }
            
            return ordered.toArray(new Sync[ordered.size()]);
        }

        /**
         * {@inheritDoc}
         */
        public Sync getSyncForKey(Object key) {
            int hash = key == null ? 0 : hash(key.hashCode());
            return new ReadWriteLockSync(segmentFor(hash));
        }

        /**
         * {@inheritDoc}
         */
        public void unlockWriteLockForAllKeys(Object... keys) {
            for (Segment s : getSegmentsFor(keys)) {
                s.writeLock().unlock();
            }
        }
        
        private Set<Segment> getSegmentsFor(Object... keys) {
            Set<Segment> segs = new HashSet<Segment>();
            
            for (Object k : keys) {
                segs.add(segmentFor(hash(k.hashCode())));
            }
            
            return segs;
        }
    }

    /**
     * Superclass for all store iterators.
     */
    abstract class HashIterator {
        private int nextSegmentIndex;
        private Iterator<HashEntry> currentIterator;

        /**
         * Constructs a new HashIterator
         */
        HashIterator() {
            nextSegmentIndex = segments.length;
            
            while (nextSegmentIndex > 0) {
                nextSegmentIndex--;
                currentIterator = segments[nextSegmentIndex].hashIterator();
                if (currentIterator.hasNext()) {
                    return;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            if (this.currentIterator == null) {
                return false;
            }

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

        /**
         * Returns the next hash-entry - called by subclasses
         * 
         * @return next HashEntry
         */
        protected HashEntry nextEntry() {
            HashEntry item = null;

            if (currentIterator == null) {
                return null;
            }

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

        /**
         * {@inheritDoc}
         */
        public void remove() {
            currentIterator.remove();
        }
    }

    /**
     * Iterator over the store key set.
     */
    private final class KeyIterator extends HashIterator implements Iterator<Object> {
        /**
         * {@inheritDoc}
         */
        public Object next() {
            return super.nextEntry().key;
        }
    }

    /**
     * Sync implementation that wraps the segment locks
     */
    private final static class ReadWriteLockSync implements Sync {

        private final ReentrantReadWriteLock lock;
        
        private ReadWriteLockSync(ReentrantReadWriteLock lock) {
            this.lock = lock;
        }
        
        /**
         * {@inheritDoc}
         */
        public void lock(LockType type) {
            switch (type) {
                case READ:
                    lock.readLock().lock();
                    break;
                case WRITE:
                    lock.writeLock().lock();
                    break;
                default:
                    throw new IllegalArgumentException("We don't support any other lock type than READ or WRITE!");
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean tryLock(LockType type, long msec) throws InterruptedException {
            switch (type) {
                case READ:
                    return lock.readLock().tryLock(msec, TimeUnit.MILLISECONDS);
                case WRITE:
                    return lock.writeLock().tryLock(msec, TimeUnit.MILLISECONDS);
                default:
                    throw new IllegalArgumentException("We don't support any other lock type than READ or WRITE!");
            }
        }

        /**
         * {@inheritDoc}
         */
        public void unlock(LockType type) {
            switch (type) {
                case READ:
                    lock.readLock().unlock();
                    break;
                case WRITE:
                    lock.writeLock().unlock();
                    break;
                default:
                    throw new IllegalArgumentException("We don't support any other lock type than READ or WRITE!");
            }
        }

        public boolean isHeldByCurrentThread(LockType type) {
            switch (type) {
                case READ:
                    throw new UnsupportedOperationException("Querying of read lock is not supported.");
                case WRITE:
                    return lock.isWriteLockedByCurrentThread();
                default:
                    throw new IllegalArgumentException("We don't support any other lock type than READ or WRITE!");
            }
        }
        
    }
}