package net.sf.ehcache.writer;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.chm.ConcurrentHashMap;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

public class TestCacheWriter implements CacheWriter {
    private final Properties properties;
    private final ConcurrentMap<Object, Element> writtenElements = new ConcurrentHashMap<Object, Element>();

    public TestCacheWriter(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public ConcurrentMap<Object, Element> getWrittenElements() {
        return writtenElements;
    }

    public void init() {
        // nothing to do
    }

    public void dispose() throws CacheException {
        // nothing to do
    }

    private String getAdaptedKey(Object key) {
        String keyPrefix = properties.getProperty("key.prefix", "");
        String keySuffix = properties.getProperty("key.suffix", "");
        return keyPrefix + key + keySuffix;
    }
    
    public void write(Element element) throws CacheException {
        writtenElements.put(getAdaptedKey(element.getKey()), element);
    }

    public void writeAll(Collection<Element> elements) throws CacheException {
        for (Element element : elements) {
            writtenElements.put(getAdaptedKey(element.getKey()) + "-batched", element);
        }
    }

    public void delete(Object key) throws CacheException {
        writtenElements.remove(getAdaptedKey(key));
    }

    public void deleteAll(Collection<Object> keys) throws CacheException {
        for (Object key : keys) {
            writtenElements.remove(getAdaptedKey(key) + "-batched");
        }
    }

    public CacheWriter clone(Ehcache cache) throws CloneNotSupportedException {
        return null;
    }
}
