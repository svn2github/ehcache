package net.sf.ehcache.pool;

/**
 * @author Ludovic Orban
 */
public interface Pool {
    long getSize();

    PoolAccessor createPoolAccessor(PoolableStore store);
}
