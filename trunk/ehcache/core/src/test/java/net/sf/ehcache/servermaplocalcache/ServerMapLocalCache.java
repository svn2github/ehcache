/**
 * All content copyright 2010 (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package net.sf.ehcache.servermaplocalcache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListenerAdapter;

public class ServerMapLocalCache {

    private final static int CONCURRENCY = 256;
    private volatile TCObjectSelfStore tcoSelfStore;
    private volatile Cache ehcache;
    private final ReentrantReadWriteLock[] locks = new ReentrantReadWriteLock[CONCURRENCY];
    private final Object localSync = new Object();
    private static final ExecutorService evictionHandlerStage = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "eviction_handler_stage_thread_" + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    public ServerMapLocalCache(TCObjectSelfStore tcoSelfStore, Cache ehcache) {
        this.tcoSelfStore = tcoSelfStore;
        this.ehcache = ehcache;
        ehcache.getCacheEventNotificationService().registerListener(new EvictionListener(this));
        for (int i = 0; i < CONCURRENCY; i++) {
            locks[i] = new ReentrantReadWriteLock();
        }
    }

    private ReentrantReadWriteLock getLock(Object key) {
        return locks[Math.abs(key.hashCode() % CONCURRENCY)];
    }

    public void put(TCObjectSelf tcoSelf) {
        synchronized (localSync) {
            tcoSelfStore.addTCObjectSelf(tcoSelf);
            addToCache(tcoSelf);
        }
    }

    public TCObjectSelf getFromTCObjectSelfStore(Long oid) {
        synchronized (localSync) {
            return tcoSelfStore.getById(oid);
        }
    }

    // public void remove(String key) {
    // synchronized (localSync) {
    // removeFromCache(key);
    // }
    // }
    //
    // private void removeFromCache(String key) {
    // WriteLock writeLock = getLock(key).writeLock();
    // writeLock.lock();
    // try {
    // // remove key-value mapping: key->value
    // Element element = ehcache.removeAndReturnElement(key);
    // if (element != null) {
    // TCObjectSelf tcoSelf = (TCObjectSelf) element.getObjectValue();
    // if (tcoSelf != null) {
    // handleKeyValueMappingRemoved(key, tcoSelf);
    // }
    // }
    //
    // } finally {
    // writeLock.unlock();
    // }
    // }

    private void handleKeyValueMappingRemoved(String key, TCObjectSelf tcoSelf) {
        // remote remove
        tcoSelfStore.removeTCObjectSelf(tcoSelf);
        if (tcoSelf != null) {
            // clean up meta-mapping: id->key
            ehcache.remove(tcoSelf.getOid());
        }
    }

    private void addToCache(TCObjectSelf tcoSelf) {
        ReentrantReadWriteLock lock = getLock(tcoSelf.getKey());
        lock.writeLock().lock();
        try {
            if (DebugUtil.DEBUG) {
                DebugUtil.debug("Add to cache: " + tcoSelf);
            }

            // add meta mapping: oid->key
            ehcache.put(new Element(tcoSelf.getOid(), tcoSelf.getKey()));

            // add the key->value mapping
            ehcache.put(new Element(tcoSelf.getKey(), tcoSelf));

        } finally {
            lock.writeLock().unlock();
        }
    }

    public void entryEvicted(Object objectKey, Object objectValue) {
        if (DebugUtil.DEBUG) {
            DebugUtil.debug("Entry evicted; evictedKey: " + objectKey + ", evictedValue: " + objectValue);
        }
        if (objectValue instanceof TCObjectSelf) {
            // key-value mapping evicted
            keyValueMappingEvicted((String) objectKey, (TCObjectSelf) objectValue);
        } else {
            // oid-> key mapping removed
            objectIdMappingEvicted((Long) objectKey, (String) objectValue);
        }
    }

    private void objectIdMappingEvicted(Long oid, String key) {
        WriteLock writeLock = getLock(key).writeLock();
        writeLock.lock();
        Element element = null;
        try {
            element = ehcache.get(key);
            if (element != null) {
                TCObjectSelf tcoSelf = (TCObjectSelf) element.getObjectValue();
                if (tcoSelf != null && tcoSelf.getOid() == oid) {
                    // clean up key-value mapping
                    ehcache.remove(key);
                    tcoSelfStore.removeTCObjectSelf(tcoSelf);
                }
            }
        } finally {
            writeLock.unlock();
        }
        if (DebugUtil.DEBUG) {
            DebugUtil.debug("[objectIdEvicted] oid: " + oid + ", key: " + key + ", was mapped to value: "
                    + (element == null ? "null" : element.getObjectValue()));
        }
    }

    private void keyValueMappingEvicted(String objectKey, TCObjectSelf objectValue) {
        WriteLock writeLock = getLock(objectKey).writeLock();
        writeLock.lock();
        try {
            if (DebugUtil.DEBUG) {
                DebugUtil.debug("[keyValueMappingEvicted] key: " + objectKey + ", value: " + objectValue);
            }
            handleKeyValueMappingRemoved(objectKey, objectValue);
        } finally {
            writeLock.unlock();
        }
    }

    private static class EvictionListener extends CacheEventListenerAdapter {
        private final ServerMapLocalCache serverMapLocalCache;

        public EvictionListener(ServerMapLocalCache serverMapLocalCache) {
            this.serverMapLocalCache = serverMapLocalCache;
        }

        @Override
        public void notifyElementExpired(Ehcache cache, final Element element) {
            if (DebugUtil.DEBUG) {
                DebugUtil.debug("[ElementExpired] expiredKey: " + element.getObjectKey() + ", expiredValue: " + element.getObjectValue());
            }
            evictionHandlerStage.submit(new Runnable() {
                public void run() {
                    serverMapLocalCache.entryEvicted(element.getObjectKey(), element.getObjectValue());
                }
            });
        }

        @Override
        public void notifyElementEvicted(Ehcache cache, final Element element) {
            if (DebugUtil.DEBUG) {
                DebugUtil.debug("[ElementEvicted] expiredKey: " + element.getObjectKey() + ", expiredValue: " + element.getObjectValue());
            }
            evictionHandlerStage.submit(new Runnable() {
                public void run() {
                    serverMapLocalCache.entryEvicted(element.getObjectKey(), element.getObjectValue());
                }
            });
        }
    }
}
