package net.sf.ehcache.writer;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.Collection;

public class TestCacheWriterException implements CacheWriter {
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

    public void delete(CacheEntry entry) throws CacheException {
        throw new UnsupportedOperationException();
    }

    public void deleteAll(Collection<CacheEntry> entries) throws CacheException {
        throw new UnsupportedOperationException();
    }

    public CacheWriter clone(Ehcache cache) throws CloneNotSupportedException {
        return null;
    }
}