package net.sf.ehcache.writer.writebehind;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.writer.CacheWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Alex Snaps
 */
public class WriteBehindQueueManager implements WriteBehind {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private List<WriteBehindQueue> queues = new ArrayList<WriteBehindQueue>();

    public WriteBehindQueueManager(CacheConfiguration config) {
        CacheWriterConfiguration cacheWriterConfiguration = config.getCacheWriterConfiguration();
        int writeBehindConcurrency = cacheWriterConfiguration.getWriteBehindConcurrency();
        for(int i = 0; i < writeBehindConcurrency; i++) {
            this.queues.add(new WriteBehindQueue(config));
        }
    }

    public void start(final CacheWriter writer) throws CacheException {
        writeLock.lock();
        try {
            for (WriteBehindQueue queue : queues) {
                queue.start(writer);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void write(final Element element) {
        readLock.lock();
        try {
            getQueue(element.getKey()).write(element);
        } finally {
            readLock.unlock();
        }
    }

    private WriteBehindQueue getQueue(final Object key) {
        return queues.get(Math.abs(key.hashCode() % queues.size()));
    }

    public void delete(final CacheEntry entry) {
        readLock.lock();
        try {
            getQueue(entry.getKey()).delete(entry);
        } finally {
            readLock.unlock();
        }
    }

    public void setOperationsFilter(final OperationsFilter filter) {
        readLock.lock();
        try {
            for (WriteBehindQueue queue : queues) {
                queue.setOperationsFilter(filter);
            }
        } finally {
            readLock.unlock();
        }
    }

    public void stop() throws CacheException {
        writeLock.lock();
        try {
            for (WriteBehindQueue queue : queues) {
                queue.stop();
            }
        } finally {
            writeLock.unlock();
        }
    }
}
