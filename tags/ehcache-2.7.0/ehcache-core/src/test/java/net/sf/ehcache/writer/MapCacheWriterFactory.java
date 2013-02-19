package net.sf.ehcache.writer;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Snaps
 */
public class MapCacheWriterFactory extends CacheWriterFactory {

    public static final ConcurrentMap<Object, Object> map = new ConcurrentHashMap<Object, Object>();
    public static final AtomicInteger writes = new AtomicInteger(0);
    public static final AtomicInteger deletes = new AtomicInteger(0);

    @Override
    public CacheWriter createCacheWriter(final Ehcache cache, final Properties properties) {
        return new CacheWriter() {
            @Override
            public CacheWriter clone(final Ehcache cache) throws CloneNotSupportedException {
                throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
            }

            @Override
            public void init() {

            }

            @Override
            public void dispose() throws CacheException {

            }

            @Override
            public void write(final Element element) throws CacheException {
                System.out.println("PUT " + element);
                map.put(element.getKey(), element);
                writes.getAndIncrement();
            }

            @Override
            public void writeAll(final Collection<Element> elements) throws CacheException {
                for (Element element : elements) {
                    write(element);
                }
            }

            @Override
            public void delete(final CacheEntry entry) throws CacheException {
                System.out.println("DELETE " + entry);
                map.remove(entry.getKey());
                deletes.getAndIncrement();
            }

            @Override
            public void deleteAll(final Collection<CacheEntry> entries) throws CacheException {
                for (CacheEntry entry : entries) {
                    delete(entry);
                }
            }

            @Override
            public void throwAway(final Element element, final SingleOperationType operationType, final RuntimeException e) {
                throw new UnsupportedOperationException("Someone... i.e. YOU! should think about implementing this someday!");
            }
        };
    }
}
