package net.sf.ehcache.store.disk;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.compound.factories.DiskOverflowStorageFactory;
import net.sf.ehcache.store.compound.factories.DiskPersistentStorageFactory;
import net.sf.ehcache.store.compound.impl.DiskPersistentStore;
import net.sf.ehcache.store.compound.impl.OverflowToDiskStore;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Alex Snaps
 */
public class DiskStoreHelper {

    private static final Callable<Void> NOOP_CALLABLE = new Callable<Void>() {

        public Void call() throws Exception {
            return null;
        }
    };

    public static Future<Void> flushAllEntriesToDisk(final Cache cache) {
        CacheStoreHelper cacheStoreHelper = new CacheStoreHelper(cache);
        return flushAllEntriesToDisk(cacheStoreHelper.getStore());
    }

    public static Future<Void> flushAllEntriesToDisk(final Store store) {
        if (store instanceof OverflowToDiskStore) {
            DiskOverflowStorageFactory factory = getField("diskFactory", store);
            ExecutorService executor = getField("diskWriter", factory);
            return executor.submit(NOOP_CALLABLE);
        } else if (store instanceof DiskPersistentStore) {
            DiskPersistentStorageFactory factory = getField("disk", store);
            ExecutorService executor = getField("diskWriter", factory);
            return executor.submit(NOOP_CALLABLE);
        } else {
            return null;
        }
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
