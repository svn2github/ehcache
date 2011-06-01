package net.sf.ehcache.pool;

/**
 * @author Ludovic Orban
 */
public interface Pool {

    /**
     * @return total size of the pool.
     */
    long getSize();

    /**
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor createPoolAccessor(PoolableStore store);

    /**
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor createPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine);

}
