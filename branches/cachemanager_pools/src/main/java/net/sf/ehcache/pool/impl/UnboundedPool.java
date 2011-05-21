package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.Role;

/**
 * @author Ludovic Orban
 */
public class UnboundedPool implements Pool {

    public UnboundedPool() {
    }

    public long getSize() {
        return -1L;
    }

    public PoolAccessor createPoolAccessor(PoolableStore store) {
        return new UnboundedPoolAccessor();
    }

    public class UnboundedPoolAccessor implements PoolAccessor {

        public UnboundedPoolAccessor() {
        }

        public long add(Object key, Object value, Object container, boolean force) {
            return 0L;
        }

        public long delete(Object key, Object value, Object container) {
            return 0L;
        }

        public long replace(Role role, Object current, Object replacement, boolean force) {
            return 0L;
        }

        public long getSize() {
            return -1L;
        }

        public void unlink() {
        }

        public void clear() {
        }
    }

}
