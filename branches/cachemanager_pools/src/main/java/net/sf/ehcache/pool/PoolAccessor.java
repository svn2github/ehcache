package net.sf.ehcache.pool;

/**
 * @author Ludovic Orban
 */
public interface PoolAccessor {

    /**
     * @return how many bytes have been added to the pool or -1 if add failed.
     */
    long add(Object key, Object value, Object container, boolean force);

    /**
     * @return how many bytes have been freed from the pool.
     */
    long delete(Object key, Object value, Object container);

    /**
     * @return how many bytes have been freed from the pool, may be negative. Long.MAX_VALUE is returned if replace failed.
     */
    long replace(Role role, Object current, Object replacement, boolean force);

    /**
     * @return how many bytes this accessor consumes from the pool.
     */
    long getSize();

    /**
     * unlink this PoolAccessor from its pool.
     */
    void unlink();

    /**
     * Free memory used by this accessor.
     */
    void clear();
}
