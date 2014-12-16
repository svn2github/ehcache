package net.sf.ehcache.terracotta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.DiskStorePathManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.junit.Test;
import org.mockito.Matchers;

/**
 * @author Alex Snaps
 */
public class TerracottaBootstrapCacheLoaderTest {

    private static final String DIRECTORY = System.getProperty("java.io.tmpdir") + "/TerracottaBootstrapCacheLoaderTest/dumps";
    private static final String MOCKED_CACHE_NAME = "MockedCache";

    private final TerracottaBootstrapCacheLoader cacheLoader = new TerracottaBootstrapCacheLoader(false, DIRECTORY, false);

    @Test
    public void testComplainsOnNonTcClusteredCacheButDoesNotFail() {
        cacheLoader.load(new Cache(new CacheConfiguration("test", 0)));
    }

    @Test(expected = CacheException.class)
    public void testFailsWhenCacheIsNotAlive() {
        cacheLoader.load(new Cache(new CacheConfiguration("test", 0).terracotta(new TerracottaConfiguration())));
    }

    @Test
    public void testBootstrapsWhenNoSnapshotPresent() {
        final Ehcache cache = mockCacheToBootStrap();
        cacheLoader.load(cache);
        verify(cache, never()).get(Matchers.anyObject());
    }

    @Test
    public void testDisposesProperly() {
        final Ehcache cache = mockCacheToBootStrap();
        cacheLoader.dispose();
        cacheLoader.load(cache);
        cacheLoader.dispose();
    }

    @Test
    public void testBootstrapsWhenSnapshotPresent() throws Exception {

        DiskStorePathManager pathManager = getDiskStorePathManager(cacheLoader);
        RotatingSnapshotFile file = new RotatingSnapshotFile(pathManager, MOCKED_CACHE_NAME, getClass().getClassLoader());

        // Duplicated keys should be filtered out!
        final List<Integer> localKeys = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 9);
        file.writeAll(localKeys);
        final Ehcache cache = mockCacheToBootStrap();
        cacheLoader.load(cache);
        verify(cache, times(new HashSet<Integer>(localKeys).size())).get(Matchers.anyObject());
        for (Integer localKey : localKeys) {
            verify(cache, times(1)).get((Object) localKey);
        }
        file.currentSnapshotFile().delete();
    }

    private Ehcache mockCacheToBootStrap() {
        final Ehcache cache = mock(Ehcache.class);
        CacheConfiguration cacheConfiguration = mock(CacheConfiguration.class);
        when(cacheConfiguration.isTerracottaClustered()).thenReturn(true);
        when(cache.getName()).thenReturn(MOCKED_CACHE_NAME);
        when(cache.getCacheConfiguration()).thenReturn(cacheConfiguration);
        when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cache.getCacheManager()).thenReturn(cacheManager);
        when(cache.get(Matchers.<Object>anyObject())).thenReturn(null);
        when(cacheManager.getConfiguration()).thenReturn(new Configuration());
        return cache;
    }

    private DiskStorePathManager getDiskStorePathManager(TerracottaBootstrapCacheLoader loader) throws Exception {
        Field field = TerracottaBootstrapCacheLoader.class.getDeclaredField("diskStorePathManager");
        field.setAccessible(true);
        return (DiskStorePathManager)field.get(loader);
    }

}
