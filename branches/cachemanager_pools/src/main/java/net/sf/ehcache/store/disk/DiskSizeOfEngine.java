package net.sf.ehcache.store.disk;

import net.sf.ehcache.pool.SizeOfEngine;

/**
 * @author Ludovic Orban
 */
public class DiskSizeOfEngine implements SizeOfEngine {
    public long sizeOf(Object key, Object value, Object container) {
        if (container != null && !(container instanceof DiskStorageFactory.DiskMarker)) {
            throw new IllegalArgumentException("can only size DiskStorageFactory.DiskMarker");
        }

        if (container == null) {
            return 0;
        }

        DiskStorageFactory.DiskMarker marker = (DiskStorageFactory.DiskMarker) container;
        return marker.getSize();
    }
}
