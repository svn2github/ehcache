package net.sf.ehcache.openjpa.datacache;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;

import junit.framework.TestCase;

public class TestEhCache extends TestCase {
    private static EntityManagerFactory emf;
    private static OpenJPAConfiguration conf;
    private static final String UNIT_NAME = System.getProperty("unit","test");
    
    @Override
    protected void setUp() throws Exception {
        if (emf == null) {
        	Properties props = new Properties();
        	props.put("openjpa.MetaDataFactory","jpa(Types=net.sf.ehcache.openjpa.datacache.QObject;net.sf.ehcache.openjpa.datacache.PObject)");
        	props.put("openjpa.ConnectionDriverName","org.hsqldb.jdbcDriver");
        	props.put("openjpa.ConnectionURL","jdbc:hsqldb:mem:testdb");
        	props.put("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        	props.put("openjpa.Log", "SQL=WARN");
        	props.put("openjpa.DataCacheManager","ehcache");
        	props.put("openjpa.QueryCache","ehcache");
            emf = Persistence.createEntityManagerFactory(UNIT_NAME,props);
            conf = ((OpenJPAEntityManagerFactorySPI)emf).getConfiguration();
        }
    }

    /**
     * Verify that configuration matches expectation.
     */
    public void testCacheConfiguration() {
        DataCacheManager dcm = conf.getDataCacheManagerInstance();
        String dcmName = conf.getDataCacheManager();
        DataCache systemCache = dcm.getSystemDataCache();
        String dataCache =  conf.getDataCache();
        
        assertNotNull(systemCache);
        assertNotNull(dcm);
        assertEquals(EhCacheDerivation.EHCACHE, dataCache); 
        assertEquals(EhCacheDerivation.EHCACHE, dcmName);
        assertTrue(dcm instanceof EhCacheDataCacheManager);
        assertTrue(systemCache instanceof EhCacheDataCache);
        
        EhCacheDataCacheManager tdcm = (EhCacheDataCacheManager)dcm;
        
        /*assertEquals(getConfiguredDataCacheName(PObject.class),
               tdcm.getEhCache(PObject.class).getName());
        assertEquals(getConfiguredDataCacheName(PObject.class),
               tdcm.getEhCache(PObject.class).getName());
        assertNotSame(tdcm.getEhCache(PObject.class), 
                tdcm.getEhCache(QObject.class));
        assertSame(tdcm.getEhCache(QObject.class), 
                tdcm.getEhCache(QObject.class));
        */
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
