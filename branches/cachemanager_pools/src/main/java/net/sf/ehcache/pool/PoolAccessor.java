package net.sf.ehcache.pool;

/**
 * @author Ludovic Orban
 */
public interface PoolAccessor {

    /**
     * @return how many bytes have been added to the pool.
     */
    long add(Object key, Object value, Object container);

    /**
     * @return how many bytes have been freed from the pool.
     */
    long delete(Object key, Object value, Object container);

    /**
     * @return how many bytes have been freed from the pool, may be negative.
     */
    long replace(Role role, Object current, Object replacement);

    /**
     * @return how many bytes this accessor consumes from the pool.
     */
    long getSize();

}
