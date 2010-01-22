package net.sf.ehcache.writer;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.Collection;

public class WriteThroughTestCacheWriterException implements CacheWriter {
    public void init() {
        // nothing to do
    }

    public void dispose() throws CacheException {
        // nothing to do
    }
    
    public void write(Element element) throws CacheException {
        throw new UnsupportedOperationException();
    }

    public void writeAll(Collection<Element> elements) throws CacheException {
        throw new UnsupportedOperationException();
    }

    public void delete(Object key) throws CacheException {
        throw new UnsupportedOperationException();
    }

    public void deleteAll(Collection<Object> keys) throws CacheException {
        throw new UnsupportedOperationException();
    }

    public CacheWriter clone(Ehcache cache) throws CloneNotSupportedException {
        return null;
    }
}