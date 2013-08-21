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

package net.sf.ehcache.writer.writebehind;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.writer.CacheWriter;

/**
 * @author Alex Snaps
 */
public class WriteBehindQueueManager implements WriteBehind {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private final List<WriteBehind> queues = new ArrayList<WriteBehind>();

    /**
     * Create the write behind queue manager with queues created via the passed in {@link WriteBehindQueueFactory}
     *
     * @param config configuration for the cache this write behind queue manager is working with
     * @param queueFactory factory used to create the write behind queues.
     */
    protected WriteBehindQueueManager(CacheConfiguration config, WriteBehindQueueFactory queueFactory) {
      CacheWriterConfiguration cacheWriterConfiguration = config.getCacheWriterConfiguration();
      int writeBehindConcurrency = cacheWriterConfiguration.getWriteBehindConcurrency();
      for (int i = 0; i < writeBehindConcurrency; i++) {
        this.queues.add(queueFactory.createQueue(i, config));
      }
    }

    /**
     * Create a new write behind queue manager. Which in turn will create as many queues as
     * required by the {@link net.sf.ehcache.config.CacheWriterConfiguration#getWriteBehindConcurrency}
     *
     * @param config the configuration for the queue
     */
    public WriteBehindQueueManager(CacheConfiguration config) {
      this(config, new WriteBehindQueueFactory());
    }

    /**
     * {@inheritDoc}
     */
    public void start(final CacheWriter writer) throws CacheException {
        writeLock.lock();
        try {
            for (WriteBehind queue : queues) {
                queue.start(writer);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void write(final Element element) {
        readLock.lock();
        try {
            getQueue(element.getKey()).write(element);
        } finally {
            readLock.unlock();
        }
    }

    private WriteBehind getQueue(final Object key) {
        return queues.get(Math.abs(key.hashCode() % queues.size()));
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final CacheEntry entry) {
        readLock.lock();
        try {
            getQueue(entry.getKey()).delete(entry);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setOperationsFilter(final OperationsFilter filter) {
        readLock.lock();
        try {
            for (WriteBehind queue : queues) {
                queue.setOperationsFilter(filter);
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws CacheException {
        writeLock.lock();
        try {
            for (WriteBehind queue : queues) {
                queue.stop();
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getQueueSize() {
        int size = 0;
        readLock.lock();
        try {
            for (WriteBehind queue : queues) {
                size += queue.getQueueSize();
            }
        } finally {
            readLock.unlock();
        }
        return size;
    }

    /**
     * Factory used to create write behind queues.
     */
    protected static class WriteBehindQueueFactory {
      /**
       * Create a write behind queue stripe.
       *
       * @param index index of the stripe
       * @param config cache configuration for the cache this queue will be associated with.
       * @return a write behind queue
       */
      protected WriteBehind createQueue(int index, CacheConfiguration config) {
        return new WriteBehindQueue(config);
      }
    }
}
