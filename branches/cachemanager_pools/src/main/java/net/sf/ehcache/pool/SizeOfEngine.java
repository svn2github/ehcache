package net.sf.ehcache.pool;

/**
 * @author Ludovic Orban
 */
public interface SizeOfEngine {
    long sizeOf(Object key, Object value, Object container);
}
