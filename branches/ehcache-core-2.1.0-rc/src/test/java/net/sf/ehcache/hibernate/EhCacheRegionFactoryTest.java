package net.sf.ehcache.hibernate;

import net.sf.ehcache.util.ClassLoaderUtil;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
            fail("This should have succeeded");
        }
        assertNotNull("Session factory should have been successfully created!", sessionFactory);
        sessionFactory.close();
    }

    @AfterClass
    public static void tearDown() throws Exception {
    }
}
