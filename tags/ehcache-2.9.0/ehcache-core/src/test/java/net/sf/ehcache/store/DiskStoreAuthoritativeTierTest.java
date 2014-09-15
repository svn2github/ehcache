package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.DiskStoreTest;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.disk.DiskStore;

/**
 * @author Alex Snaps
 */
public class DiskStoreAuthoritativeTierTest extends AbstractAuthoritativeTierTest<DiskStore> {

    @Override
    protected DiskStore createAuthoritativeTier(CacheManager cacheManager) {
      Cache cache = new Cache("test/NonPersistent", 1, true, false, 2, 1, false, 1);
      cacheManager.addCache(cache);
      try {
          return DiskStoreTest.getDiskStore(new CacheStoreHelper(cache).getStore());
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
    }

    @Override
    protected boolean isFaulted(final Object key, final DiskStore diskStore) {
        return diskStore.isFaulted(key);
    }

  @Override
  protected CacheManager createCacheManager() {
    Configuration configuration = new Configuration().name(DiskStoreAuthoritativeTierTest.class.getSimpleName());
    return new CacheManager(configuration);
  }
}
