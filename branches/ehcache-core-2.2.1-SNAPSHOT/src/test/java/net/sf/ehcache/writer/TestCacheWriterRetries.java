package net.sf.ehcache.writer;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TestCacheWriterRetries extends AbstractTestCacheWriter {
    private final int retries;
    private final Map<Object, Element> writtenElements = new HashMap<Object, Element>();
    private final Map<Object, Integer> retryCount = new HashMap<Object, Integer>();
    private final Map<Object, Integer> writeCount = new HashMap<Object, Integer>();
    private final Map<Object, Integer> deleteCount = new HashMap<Object, Integer>();

    public TestCacheWriterRetries(int retries) {
        this.retries = retries;
    }

    public Map<Object, Element> getWrittenElements() {
        return writtenElements;
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
        if (!deleteCount.containsKey(key)) {
            writtenElements.put(key, element);
        }
        increaseWriteCount(key);
    }

    @Override
    public synchronized void write(Element element) throws CacheException {
        final Object key = element.getObjectKey();
        failUntilNoMoreRetries(key);
        put(key, element);
    }

    @Override
    public synchronized void writeAll(Collection<Element> elements) throws CacheException {
        Iterator<Element> it = elements.iterator();
        while (it.hasNext()) {
            Element element = it.next();
            // fail on the last item in the batch
            final Object key = element.getObjectKey();
            if (!it.hasNext()) {
                failUntilNoMoreRetries(key);
            }
            put(key, element);
        }
    }

    private void remove(Object key) {
        writtenElements.remove(key);
        increaseDeleteCount(key);
    }

    @Override
    public synchronized void delete(CacheEntry entry) throws CacheException {
        Object key = entry.getKey();
        failUntilNoMoreRetries(key);
        remove(key);
    }

    @Override
    public synchronized void deleteAll(Collection<CacheEntry> entries) throws CacheException {
        Iterator<CacheEntry> it = entries.iterator();
        while (it.hasNext()) {
            CacheEntry entry = it.next();
            Object key = entry.getKey();
            if (!it.hasNext()) {
                failUntilNoMoreRetries(key);
            }
            remove(key);
        }
    }
}