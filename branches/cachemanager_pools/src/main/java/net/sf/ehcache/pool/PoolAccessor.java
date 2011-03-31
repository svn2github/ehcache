package net.sf.ehcache.pool;

/**
 * @author Ludovic Orban
 */
public interface PoolAccessor {

    boolean add(Object key, Object value, Object container);

    void delete(Object key, Object value, Object container);

    void replace(Role role, Object current, Object replacement);

    long getSize();

}
