package net.sf.ehcache.store.disk;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.store.DiskBackedMemoryStore;
import net.sf.ehcache.store.Store;

import java.lang.reflect.Field;
import java.util.concurrent.Future;

/**
 * @author Alex Snaps
 */
public class PerfDiskStoreHelper {

    public static Future<Void> flushAllEntriesToDisk(final Cache cache) {
        CacheStoreHelper cacheStoreHelper = new CacheStoreHelper(cache);
        final Store store = cacheStoreHelper.getStore();
        if(store instanceof DiskBackedMemoryStore) {
            final DiskStore authority = getField("authority", store);
            final DiskStorageFactory factory = getField("disk", authority);
            return factory.flush();
        }
        return null;
    }

    private static <T> T getField(final String fieldName, final Object obj) {
        try {
            Field field = null;
            Class clazz = obj.getClass();
            while(field == null && clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                } catch (SecurityException e) {
                    throw new RuntimeException(e);
                }
            }
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
