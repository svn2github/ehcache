/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.openjpa.datacache;

import java.lang.reflect.Method;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.StoreCacheImpl;

import junit.framework.TestCase;

/**
 * @author Craig Andrews
 * @author Greg Luck
 */
public class TestEhCache extends TestCase {
    private static EntityManagerFactory emf;
    private static OpenJPAConfiguration conf;
    private static final String UNIT_NAME = System.getProperty("unit", "test");

    @Override
    protected void setUp() throws Exception {
        if (emf == null) {
            Properties props = new Properties();
            props.put("openjpa.MetaDataFactory", "jpa(Types=net.sf.ehcache.openjpa.datacache.QObject;" +
                    "net.sf.ehcache.openjpa.datacache.PObject;" +
                    "net.sf.ehcache.openjpa.datacache.SubQObject)");
            props.put("openjpa.ConnectionDriverName", "org.hsqldb.jdbcDriver");
            props.put("openjpa.ConnectionURL", "jdbc:hsqldb:mem:testdb");
            props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
            props.put("openjpa.Log", "SQL=WARN");
            props.put("openjpa.DataCacheManager", "ehcache");
            emf = Persistence.createEntityManagerFactory(UNIT_NAME, props);
            conf = ((OpenJPAEntityManagerFactorySPI) emf).getConfiguration();
        }
    }

    /**
     * Verify that configuration matches expectation.
     */
    public void testCacheConfiguration() {
        DataCacheManager dcm = conf.getDataCacheManagerInstance();
        String dcmName = conf.getDataCacheManager();
        DataCache systemCache = dcm.getSystemDataCache();
        String dataCache = conf.getDataCache();

        assertNotNull(systemCache);
        assertNotNull(dcm);
        assertEquals(EhCacheDerivation.EHCACHE, dataCache);
        assertEquals(EhCacheDerivation.EHCACHE, dcmName);
        assertTrue(dcm instanceof EhCacheDataCacheManager);
        assertTrue(systemCache instanceof EhCacheDataCache);

        EhCacheDataCacheManager tdcm = (EhCacheDataCacheManager) dcm;

        assertEquals("default",
            getConfiguredDataCacheName(QObject.class));
        assertEquals(QObject.class.getName(),
               tdcm.getEhCache(QObject.class).getName());
        assertEquals(getConfiguredDataCacheName(PObject.class),
               tdcm.getEhCache(PObject.class).getName());
        assertNotSame(tdcm.getEhCache(PObject.class),
                tdcm.getEhCache(QObject.class));
        assertNotSame(tdcm.getEhCache(SubQObject.class),
                tdcm.getEhCache(QObject.class));
        assertSame(tdcm.getEhCache(QObject.class),
                tdcm.getEhCache(QObject.class));
    }

    public void testPersist() {
        EntityManager em = emf.createEntityManager();
        PObject pc = new PObject("XYZ");
        em.getTransaction().begin();
        em.persist(pc);
        em.getTransaction().commit();
        Object oid = pc.getId();

        em.clear();
        // After clean the instance must not be in L1 cache
        assertFalse(em.contains(pc));
        // But it must be found in L2 cache by its OpenJPA identifier
        assertTrue(getCache(pc.getClass()).contains(getOpenJPAId(pc, oid)));

        PObject pc2 = em.find(PObject.class, oid);
        // After find(), the original instance is not in the L1 cache
        assertFalse(em.contains(pc));
        // After find(), the found instance is in the L1 cache
        assertTrue(em.contains(pc2));
        // The L2 cache must still hold the key   
        assertTrue(getCache(pc.getClass()).contains(getOpenJPAId(pc, oid)));
    }

    public void testClearCache() {
        EntityManager entityManager = emf.createEntityManager();
        EntityTransaction tx = entityManager.getTransaction();
        tx.begin();
        SubQObject subQObject = new SubQObject("one", "two");
        QObject qObject = new QObject("one");
        PObject pObject = new PObject("one");
        entityManager.persist(subQObject);
        entityManager.persist(qObject);
        entityManager.persist(pObject);
        tx.commit();
        assertTrue(getCache(subQObject.getClass()).contains(getOpenJPAId(subQObject, subQObject.getId())));
        assertTrue(getCache(qObject.getClass()).contains(getOpenJPAId(qObject, qObject.getId())));
        assertTrue(getCache(pObject.getClass()).contains(getOpenJPAId(pObject, pObject.getId())));
        evictAllOfType(qObject.getClass(), false);
        assertFalse("QObject entries should be all gone",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(qObject.getClass(), qObject.getId()));
        assertTrue("SubQObject entries should still be in the cache",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(subQObject.getClass(), subQObject.getId()));
        assertTrue("This PObject object should still be in the cache",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(pObject.getClass(), pObject.getId()));
        tx = entityManager.getTransaction();
        tx.begin();
        qObject = new QObject("two");
        entityManager.persist(qObject);
        tx.commit();
        evictAllOfType(qObject.getClass(), true);
        assertFalse("QObject entries should be all gone",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(qObject.getClass(), qObject.getId()));
        assertFalse("SubQObject entries should be all gone",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(subQObject.getClass(), subQObject.getId()));
        assertTrue("This PObject object should still be in the cache",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(pObject.getClass(), pObject.getId()));
        tx = entityManager.getTransaction();
        tx.begin();
        qObject = new QObject("three");
        entityManager.persist(qObject);
        subQObject = new SubQObject("two", "two");
        entityManager.persist(subQObject);
        tx.commit();
        evictAllOfType(subQObject.getClass(), false);
        assertTrue("QObject entries should still be in the cache",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(qObject.getClass(), qObject.getId()));
        assertFalse("SubQObject entries should be all gone",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(subQObject.getClass(), subQObject.getId()));
        assertTrue("This PObject object should still be in the cache",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(pObject.getClass(), pObject.getId()));
        tx = entityManager.getTransaction();
        tx.begin();
        subQObject = new SubQObject("three", "three");
        entityManager.persist(subQObject);
        tx.commit();
        evictAllOfType(pObject.getClass(), true);
        assertTrue("QObject entries should still be in the cache",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(qObject.getClass(), qObject.getId()));
        assertTrue("SubQObject entries should still be in the cache",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(subQObject.getClass(), subQObject.getId()));
        assertFalse("This PObject object should be gone",
            OpenJPAPersistence.cast(emf).getStoreCache().contains(pObject.getClass(), pObject.getId()));
    }

    public void testGetDataCache() {

    }

    private void evictAllOfType(final Class aClass, final boolean subClasses) {
        OpenJPAEntityManagerFactory openJPAEntityManagerFactory = OpenJPAPersistence.cast(emf);
        DataCache cache = ((StoreCacheImpl)openJPAEntityManagerFactory.getStoreCache()).getDelegate();
        if(cache instanceof EhCacheDataCache) {
            EhCacheDataCache ehCache = (EhCacheDataCache)cache;
            try {
                Method method = EhCacheDataCache.class.getDeclaredMethod("removeAllInternal", Class.class, Boolean.TYPE);
                method.setAccessible(true);
                method.invoke(ehCache, aClass, subClasses);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Gets the data cache for the given class.
     */
    DataCache getCache(Class cls) {
        String name = getConfiguredDataCacheName(cls);
        return conf.getDataCacheManagerInstance().getDataCache(name);
    }

    /**
     * Gest the configured name of the cache for the given class.
     */
    String getConfiguredDataCacheName(Class cls) {
        MetaDataRepository mdr = conf.getMetaDataRepositoryInstance();
        ClassMetaData meta = mdr.getMetaData(cls, null, true);
        return meta.getDataCacheName();
    }

    Object getOpenJPAId(Object pc, Object oid) {
        ClassMetaData meta = conf.getMetaDataRepositoryInstance()
                .getCachedMetaData(pc.getClass());
        assertNotNull(meta);
        Object ooid = JPAFacadeHelper.toOpenJPAObjectId(meta, oid);
        assertNotNull(oid);
        return ooid;
    }
}
