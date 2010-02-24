package net.sf.ehcache.writer;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestCacheWriter extends AbstractCacheWriter {
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

    public synchronized void write(Element element) throws CacheException {
        writtenElements.put(getAdaptedKey(element.getObjectKey()), element);
    }

    public synchronized void writeAll(Collection<Element> elements) throws CacheException {
        for (Element element : elements) {
            writtenElements.put(getAdaptedKey(element.getObjectKey()) + "-batched", element);
        }
    }

    public synchronized void delete(CacheEntry entry) throws CacheException {
        deletedElements.put(getAdaptedKey(entry.getKey()), entry.getElement());
    }

    public synchronized void deleteAll(Collection<CacheEntry> entries) throws CacheException {
        for (CacheEntry entry : entries) {
            deletedElements.put(getAdaptedKey(entry.getKey()) + "-batched", entry.getElement());
        }
    }
}
