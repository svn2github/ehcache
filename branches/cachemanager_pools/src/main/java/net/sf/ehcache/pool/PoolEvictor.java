package net.sf.ehcache.pool;

import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public interface PoolEvictor<T> {
    boolean freeSpace(Collection<T> from, long bytes);
}
