package net.sf.ehcache.config;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class CacheConfigurationTest {

    private CacheManager cacheManager;

    @Before
    public void setup() {
        this.cacheManager = CacheManager.getInstance();
    }

    @Test
    public void testDiskStorePath() {

        String name = "testTemp";
        String path = "c:\\something\\temp";

        CacheConfiguration cacheConfiguration = new CacheConfiguration()
            .name(name)
            .diskStorePath(path)
            .diskPersistent(true);
        cacheManager.addCache(new Cache(cacheConfiguration));
        Cache cache = cacheManager.getCache(name);
        assertThat(getDiskStorePath(cache), equalTo(path));
        cache.put(new Element("KEY", "VALUE"));
    }

    private String getDiskStorePath(final Cache cache) {
        try {
            Field declaredField = Cache.class.getDeclaredField("diskStorePath");
            declaredField.setAccessible(true);
            return (String)declaredField.get(cache);
        } catch (Exception e) {
            throw new RuntimeException("Did you rename Cache.diskStorePath ?!", e);
        }
    }
}
