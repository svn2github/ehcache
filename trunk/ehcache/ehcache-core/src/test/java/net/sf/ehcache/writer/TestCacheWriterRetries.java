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

package net.sf.ehcache.writer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

public class TestCacheWriterRetries extends AbstractTestCacheWriter {
    private final int retries;
    private final List<WriterEvent> writerEvents = new ArrayList<WriterEvent>();
    private final Map<Object, Integer> retryCount = new HashMap<Object, Integer>();
    private final Map<Object, Integer> writeCount = new HashMap<Object, Integer>();
    private final Map<Object, Integer> deleteCount = new HashMap<Object, Integer>();
    private final Map<SingleOperationType, List<Element>> thrownAwayElements =  new HashMap<SingleOperationType, List<Element>>();
    private volatile boolean throwing = true;

    {
        for (SingleOperationType singleOperationType : SingleOperationType.values()) {
            thrownAwayElements.put(singleOperationType, new ArrayList<Element>());
        }
    }

    public TestCacheWriterRetries(int retries) {
        this.retries = retries;
    }

    public List<WriterEvent> getWriterEvents() {
        return writerEvents;
    }

    public Map<Object, Integer> getWriteCount() {
        return writeCount;
    }

    public Map<Object, Integer> getDeleteCount() {
        return deleteCount;
    }

    private void failUntilNoMoreRetries(Object key) {
        int remainingRetries;
        if (!retryCount.containsKey(key)) {
            remainingRetries = retries;
        } else {
            remainingRetries = retryCount.get(key);
        }
        if (remainingRetries-- > 0) {
            retryCount.put(key, remainingRetries);
            throw new RuntimeException("Throwing exception to test retries, " + remainingRetries + " remaining for " + key);
        }
        retryCount.remove(key);
    }

    private void increaseWriteCount(Object key) {
        if (!writeCount.containsKey(key)) {
            writeCount.put(key, 1);
        } else {
            writeCount.put(key, writeCount.get(key) + 1);
        }
    }

    private void increaseDeleteCount(Object key) {
        if (!deleteCount.containsKey(key)) {
            deleteCount.put(key, 1);
        } else {
            deleteCount.put(key, deleteCount.get(key) + 1);
        }
    }

    private void put(Object key, Element element) {
        writerEvents.add(new WriterEvent(element));
        increaseWriteCount(key);
    }

    @Override
    public synchronized void write(Element element) throws CacheException {
        final Object key = element.getObjectKey();
        if (throwing) {
            failUntilNoMoreRetries(key);
        }
        put(key, element);
    }

    @Override
    public synchronized void writeAll(Collection<Element> elements) throws CacheException {
        Iterator<Element> it = elements.iterator();
        while (it.hasNext()) {
            Element element = it.next();
            // fail on the last item in the batch
            final Object key = element.getObjectKey();
            if (!it.hasNext() && throwing) {
                failUntilNoMoreRetries(key);
            }
            put(key, element);
        }
    }

    private void remove(Object key) {
        writerEvents.add(new WriterEvent(key));
        increaseDeleteCount(key);
    }

    @Override
    public synchronized void delete(CacheEntry entry) throws CacheException {
        Object key = entry.getKey();
        if (throwing) {
            failUntilNoMoreRetries(key);
        }
        remove(key);
    }

    @Override
    public synchronized void deleteAll(Collection<CacheEntry> entries) throws CacheException {
        Iterator<CacheEntry> it = entries.iterator();
        while (it.hasNext()) {
            CacheEntry entry = it.next();
            Object key = entry.getKey();
            if (!it.hasNext() && throwing) {
                failUntilNoMoreRetries(key);
            }
            remove(key);
        }
    }

    @Override
    public void throwAway(final Element element, final SingleOperationType operationType, final RuntimeException e) {
        thrownAwayElements.get(operationType).add(element);
    }

    public void setThrowing(final boolean throwing) {
        this.throwing = throwing;
    }

    public boolean isThrowing() {
        return throwing;
    }

    public List<Element> getThrownAwayElements(SingleOperationType type) {
        return Collections.unmodifiableList(thrownAwayElements.get(type));
    }

    class WriterEvent {

        private final Object removedKey;
        private final Element addedElement;
        private final long time;
        private final int writtenSize;
        private final Map<Object, Integer> writeCount;
        private final Map<Object, Integer> deleteCount;

        WriterEvent(Object key) {
            this.removedKey = key;
            this.addedElement = null;
            time = System.nanoTime();
            writtenSize = TestCacheWriterRetries.this.writerEvents.size();
            writeCount = new HashMap<Object, Integer>(TestCacheWriterRetries.this.writeCount);
            deleteCount = new HashMap<Object, Integer>(TestCacheWriterRetries.this.deleteCount);
        }

        WriterEvent(Element element) {
            this.removedKey = null;
            this.addedElement = element;
            time = System.nanoTime();
            writtenSize = TestCacheWriterRetries.this.writerEvents.size();
            writeCount = new HashMap<Object, Integer>(TestCacheWriterRetries.this.writeCount);
            deleteCount = new HashMap<Object, Integer>(TestCacheWriterRetries.this.deleteCount);
        }

        int getWrittenSize() {
            return writtenSize;
        }

        int getWriteCount(Object key) {
            Integer value = writeCount.get(key);
            if (value == null) {
                return 0;
            } else {
                return value;
            }
        }

        int getDeleteCount(Object key) {
            Integer value = deleteCount.get(key);
            if (value == null) {
                return 0;
            } else {
                return value;
            }
        }

        long getTime() {
            return time;
        }

        Element getAddedElement() {
            return addedElement;
        }

        Object getRemovedKey() {
            return removedKey;
        }

        @Override
        public String toString() {
          if (addedElement != null) {
            return "ADDED   : " + addedElement.getObjectKey() + " @ " + time;
          } else if (removedKey != null) {
            return "REMOVED : " + removedKey + " @ " + time;
          } else {
            return "UNKNOWN : @ " + time;
          }
        }
    }
}
