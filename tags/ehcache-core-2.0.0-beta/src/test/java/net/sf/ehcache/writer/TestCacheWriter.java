package net.sf.ehcache.writer;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class TestCacheWriter implements CacheWriter {
    private final Properties properties;
    private final Map<Object, Element> writtenElements = new HashMap<Object, Element>();
    private final Set<Object> deletedKeys = new HashSet<Object>();

    public TestCacheWriter(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public Map<Object, Element> getWrittenElements() {
        return writtenElements;
    }

    public Set<Object> getDeletedKeys() {
        return deletedKeys;
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
    
    public synchronized void write(Element element) throws CacheException {
        writtenElements.put(getAdaptedKey(element.getObjectKey()), element);
    }

    public synchronized void writeAll(Collection<Element> elements) throws CacheException {
        for (Element element : elements) {
            writtenElements.put(getAdaptedKey(element.getObjectKey()) + "-batched", element);
        }
    }

    public synchronized void delete(Object key) throws CacheException {
        deletedKeys.add(getAdaptedKey(key));
    }

    public synchronized void deleteAll(Collection<Object> keys) throws CacheException {
        for (Object key : keys) {
            deletedKeys.add(getAdaptedKey(key) + "-batched");
        }
    }

    public CacheWriter clone(Ehcache cache) throws CloneNotSupportedException {
        return null;
    }
}
