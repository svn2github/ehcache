package net.sf.ehcache.hibernate;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.util.ClassLoaderUtil;

import org.hamcrest.Matcher;
import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class EhCacheRegionFactoryTest {

    private static Configuration config;


    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("derby.system.home", "target/derby");
        config = new Configuration().configure("/hibernate-config/hibernate.cfg.xml");
        config.setProperty("hibernate.hbm2ddl.auto", "create");
    }

    @Test
    public void testLoadingFromOutsideTheClasspath() {
        URL resource = ClassLoaderUtil.getStandardClassLoader().getResource("hibernate-config/ehcache.xml");
        config.setProperty("net.sf.ehcache.configurationResourceName", "file://" + resource.getFile());
        SessionFactory sessionFactory = null;
        try {
            sessionFactory = config.buildSessionFactory();
        } catch (HibernateException e) {
            e.printStackTrace();
            fail("This should have succeeded");
        }
        assertNotNull("Session factory should have been successfully created!", sessionFactory);
        sessionFactory.close();
    }

    @Test
    public void testOverwritesCacheManager() throws NoSuchFieldException, IllegalAccessException {
        URL resource = ClassLoaderUtil.getStandardClassLoader().getResource("hibernate-config/ehcache.xml");
        config.setProperty("net.sf.ehcache.configurationResourceName", "file://" + resource.getFile());
        config.setProperty("net.sf.ehcache.cacheManagerName", "overwrittenCacheManagerName");
        SessionFactory sessionFactory = config.buildSessionFactory();
        final Field cache_managers_map = CacheManager.class.getDeclaredField("CACHE_MANAGERS_MAP");
        cache_managers_map.setAccessible(true);
        assertThat(((Map)cache_managers_map.get(null)).get("tc"), nullValue());
        assertThat(((Map)cache_managers_map.get(null)).get("overwrittenCacheManagerName"), notNullValue());
        sessionFactory.close();
    }

    @AfterClass
    public static void tearDown() throws Exception {
    }
}
