package net.sf.ehcache.pool;

/**
 * @author Ludovic Orban
 */
public interface PoolAccessor {

    long add(Object key, Object value, Object container);

    long delete(Object key, Object value, Object container);

    long replace(Role role, Object current, Object replacement);

    long getSize();

}
