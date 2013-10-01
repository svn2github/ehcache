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

import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriter;

/**
 * Non stop class for write behind
 *
 * @author npurwar
 *
 */
public class NonStopWriteBehind implements WriteBehind {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private WriteBehind delegate;

    private boolean isStarted = false;
    private boolean isStopped = false;

    private CacheWriter writer;
    private OperationsFilter filter;

    /**
     *
     * @param writeBehind
     */
    public void init(WriteBehind writeBehind) {
        lock.writeLock().lock();

        try {
            delegate = writeBehind;
            if (isStarted) {
                delegate.start(writer);
            }
            if (filter != null) {
                delegate.setOperationsFilter(filter);
            }
            if (isStopped) {
                delegate.stop();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void start(CacheWriter writerParam) throws CacheException {
        lock.writeLock().lock();

        try {
            if (delegate == null) {
                isStarted = true;
                NonStopWriteBehind.this.writer = writerParam;
                return;
            }
        } finally {
            lock.writeLock().unlock();
        }

        delegate.start(writerParam);
    }

    @Override
    public void write(Element element) {
        lock.readLock().lock();

        try {
            if (delegate == null) {
                throw new IllegalStateException();
            }
        } finally {
            lock.readLock().unlock();
        }

        delegate.write(element);
    }

    @Override
    public void delete(CacheEntry entry) {
        lock.readLock().lock();

        try {
            if (delegate == null) {
                throw new IllegalStateException();
            }
        } finally {
            lock.readLock().unlock();
        }

        delegate.delete(entry);
    }

    @Override
    public void setOperationsFilter(OperationsFilter filter) {
        lock.writeLock().lock();

        try {
            if (delegate == null) {
                NonStopWriteBehind.this.filter = filter;
                return;
            }
        } finally {
            lock.writeLock().unlock();
        }

        delegate.setOperationsFilter(filter);
    }

    @Override
    public void stop() throws CacheException {
        lock.writeLock().lock();

        try {
            if (delegate == null) {
                isStopped = true;
                return;
            }
        } finally {
            lock.writeLock().unlock();
        }

        delegate.stop();
    }

    @Override
    public long getQueueSize() {
        lock.readLock().lock();

        try {
            if (delegate == null) {
                return 0;
            }
        } finally {
            lock.readLock().unlock();
        }

        return delegate.getQueueSize();
    }
}
