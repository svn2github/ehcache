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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

public class TestCacheWriter extends AbstractTestCacheWriter {
    private final Properties properties;
    private final Map<Object, Element> writtenElements = new HashMap<Object, Element>();
    private final Map<Object, Element> deletedElements = new HashMap<Object, Element>();

    public TestCacheWriter(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public Map<Object, Element> getWrittenElements() {
        return writtenElements;
    }

    public Map<Object, Element> getDeletedElements() {
        return deletedElements;
    }

    private String getAdaptedKey(Object key) {
        String keyPrefix = properties.getProperty("key.prefix", "");
        String keySuffix = properties.getProperty("key.suffix", "");
        return keyPrefix + key + keySuffix;
    }

    private void throwIfDisposed() {
        if (!isInitialized()) {
            throw new IllegalStateException("Trying to perform write/delete on cache writer not initialized");
        }
    }

    @Override
    public synchronized void write(Element element) throws CacheException {
        throwIfDisposed();
        writtenElements.put(getAdaptedKey(element.getObjectKey()), element);
    }

    @Override
    public synchronized void writeAll(Collection<Element> elements) throws CacheException {
        throwIfDisposed();
        for (Element element : elements) {
            writtenElements.put(getAdaptedKey(element.getObjectKey()) + "-batched", element);
        }
    }

    @Override
    public synchronized void delete(CacheEntry entry) throws CacheException {
        throwIfDisposed();
        deletedElements.put(getAdaptedKey(entry.getKey()), entry.getElement());
    }

    @Override
    public synchronized void deleteAll(Collection<CacheEntry> entries) throws CacheException {
        throwIfDisposed();
        for (CacheEntry entry : entries) {
            deletedElements.put(getAdaptedKey(entry.getKey()) + "-batched", entry.getElement());
        }
    }
}
