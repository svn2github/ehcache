package net.sf.ehcache.xmemcached;

import net.rubyeye.xmemcached.transcoders.CachedData;

import java.io.Serializable;

/**
 * @author Ludovic Orban
 */
public class CachedDataWrapper implements Serializable {

    private final int capacity;
    private final long cas;
    private final byte[] data;
    private final int flags;

    public CachedDataWrapper(CachedData cachedData) {
        this.capacity = cachedData.getCapacity();
        this.cas = cachedData.getCas();
        this.data = cachedData.getData();
        this.flags = cachedData.getFlag();
    }

    public CachedData getCachedData() {
        return new CachedData(flags, data, capacity, cas);
    }

}
