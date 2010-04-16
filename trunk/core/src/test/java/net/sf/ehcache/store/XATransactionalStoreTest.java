package net.sf.ehcache.store;

import bitronix.tm.TransactionManagerServices;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class XATransactionalStoreTest {

    private TransactionManager transactionManager;
    private Cache              cach1;
    private Cache              cache;

    @Before
    public void setup() throws Exception {
        transactionManager = TransactionManagerServices.getTransactionManager();
        CacheManager cacheManager = CacheManager.getInstance();
        cache = cacheManager.getCache("xaCache");
        if (cache == null) {
            cache = new Cache(new CacheConfiguration("xaCache", 1000).transactionalMode(CacheConfiguration.TransactionalMode.XA));
            cacheManager.addCache(cache);
        }
        cach1 = cacheManager.getCache("otherXaCache");
        if (cach1 == null) {
            cach1 = new Cache(new CacheConfiguration("otherXaCache", 1000).transactionalMode(CacheConfiguration.TransactionalMode.XA));
            cacheManager.addCache(cach1);
        }
        transactionManager.begin();
        cache.removeAll();
        cach1.removeAll();
        transactionManager.commit();
    }

    @Test
    public void testPutIfAbsent() throws Exception {
        transactionManager.begin();
        assertEquals("Cache should be empty to start", 0, cache.getSize());
        assertEquals("Cach1 should be empty to start", 0, cach1.getSize());
        assertNull(cach1.putIfAbsent(new Element("key", "value1")));
        assertNull(cache.putIfAbsent(new Element("key", "value1")));
        Transaction tx1 = transactionManager.suspend();
        transactionManager.begin();
        assertNull(cache.putIfAbsent(new Element("key", "value2")));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("This should have thrown an Exception, as the putIfAbsent should have failed!");
        } catch (RollbackException e) {
            // Expected
        }

        CacheLockProvider clp = (CacheLockProvider)cache.getInternalContext();
        assertFalse(clp.getSyncForKey("key").isHeldByCurrentThread(LockType.WRITE));

        transactionManager.begin();
        Element element = cache.get("key");
        assertEquals("value2", element.getValue());
        assertNull(cach1.get("key"));
        assertEquals(element, cache.putIfAbsent(new Element("key", "value3")));
        transactionManager.commit();

        transactionManager.begin();
        element = cache.get("key");
        assertEquals("value2", element.getValue());
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key2", "randomValue"));
        cache.putIfAbsent(new Element("key2", "notThere!"));
        assertEquals("randomValue", cache.get("key2").getValue());
        transactionManager.commit();

        transactionManager.begin();
        assertEquals("randomValue", cache.get("key2").getValue());
        transactionManager.commit();

        transactionManager.begin();
        cach1.remove("key2");
        cache.remove("key2");
        cache.putIfAbsent(new Element("key2", "nowThere!"));
        assertEquals("nowThere!", cache.get("key2").getValue());
        transactionManager.commit();

        transactionManager.begin();
        assertEquals("nowThere!", cache.get("key2").getValue());
        transactionManager.commit();

        transactionManager.begin();
        cache.remove("key2");
        cache.putIfAbsent(new Element("key2", "nowThere!"));
        assertEquals("nowThere!", cache.get("key2").getValue());
        Transaction tx2 = transactionManager.suspend();

        transactionManager.begin();
        assertEquals("nowThere!", cache.get("key2").getValue());
        cache.put(new Element("key2", "newValue"));
        transactionManager.commit();

        transactionManager.resume(tx2);
        cach1.put(new Element("fake", "entry"));
        try {
            transactionManager.commit();
            fail("This should have thrown an Exception!");
        } catch (RollbackException e) {
            // Expected
        }

        transactionManager.begin();
        assertEquals("newValue", cache.get("key2").getValue());
        transactionManager.commit();
    }

    @Test
    public void testRemoveElement() throws Exception {
        transactionManager.begin();
        assertEquals("Cache should be empty to start", 0, cache.getSize());
        assertEquals("Cach1 should be empty to start", 0, cach1.getSize());
        assertFalse(cache.removeElement(new Element("blah", "someValue")));
        cache.put(new Element("blah", "value"));
        assertFalse(cache.removeElement(new Element("blah", "someValue")));
        transactionManager.commit();
        transactionManager.begin();
        assertNotNull(cache.get("blah"));
        assertTrue(cache.removeElement(new Element("blah", "value")));
        transactionManager.commit();
        transactionManager.begin();
        assertNull(cache.get("blah"));
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.removeElement(new Element("key", "value"));
        Transaction tx1 = transactionManager.suspend();
        transactionManager.begin();
        assertTrue(cache.removeElement(new Element("key", "value")));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element is deleted already");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNull(cache.get("key"));
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.removeElement(new Element("key", "value"));
        tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.put(new Element("key", "newValue"));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element has changed in the meantime!");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNotNull(cache.get("key"));
        assertEquals("newValue", cache.get("key").getValue());
        transactionManager.commit();
    }

    @Test
    public void testReplace() throws Exception {
        transactionManager.begin();
        assertEquals("Cache should be empty to start", 0, cache.getSize());
        assertEquals("Cach1 should be empty to start", 0, cach1.getSize());
        assertNull(cache.replace(new Element("blah", "someValue")));
        cache.put(new Element("blah", "value"));
        assertNotNull(cache.replace(new Element("blah", "someValue")));
        transactionManager.commit();
        transactionManager.begin();
        assertNotNull(cache.get("blah"));
        assertEquals("someValue", cache.get("blah").getValue());
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.replace(new Element("key", "newValue"));
        Transaction tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.remove("key");
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element is deleted already");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNull(cache.get("key"));
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.replace(new Element("key", "newValue"));
        tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.put(new Element("key", "allNewValue"));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element is deleted already");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNotNull(cache.get("key"));
        assertNotNull("allNewValue", cache.get("key").getValue());
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.replace(new Element("key", "newValue"));
        tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.put(new Element("key", "allNewValue"));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element value has changed");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNotNull(cache.get("key"));
        assertNotNull("allNewValue", cache.get("key").getValue());
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.replace(new Element("key", "value"));
        tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.put(new Element("key", "newValue"));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element has changed in the meantime!");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNotNull(cache.get("key"));
        assertEquals("newValue", cache.get("key").getValue());
        transactionManager.commit();
    }

    @Test
    public void testReplaceElement() throws Exception {
        transactionManager.begin();
        assertEquals("Cache should be empty to start", 0, cache.getSize());
        assertEquals("Cach1 should be empty to start", 0, cach1.getSize());
        assertFalse(cache.replace(new Element("blah", "someValue"), new Element("blah", "someOtherValue")));
        cache.put(new Element("blah", "value"));
        assertTrue(cache.replace(new Element("blah", "value"), new Element("blah", "someValue")));
        transactionManager.commit();
        transactionManager.begin();
        assertNotNull(cache.get("blah"));
        assertEquals("someValue", cache.get("blah").getValue());
        transactionManager.commit();

        transactionManager.begin();
        assertEquals("someValue", cache.get("blah").getValue());
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.replace(new Element("key", "value"), new Element("key", "newValue"));
        Transaction tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.remove("key");
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element is deleted already");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNull(cache.get("key"));
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        assertFalse(cache.replace(new Element("key", "wrongValue"), new Element("key", "newValue")));
        assertTrue(cache.replace(new Element("key", "value"), new Element("key", "newValue")));
        tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.put(new Element("key", "allNewValue"));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element's value has changed");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNotNull(cache.get("key"));
        assertNotNull("allNewValue", cache.get("key").getValue());
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.replace(new Element("key", "value"), new Element("key", "newValue"));
        tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.put(new Element("key", "allNewValue"));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element is deleted already");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNotNull(cache.get("key"));
        assertNotNull("allNewValue", cache.get("key").getValue());
        transactionManager.commit();

        transactionManager.begin();
        cache.put(new Element("key", "value"));
        transactionManager.commit();
        transactionManager.begin();
        cach1.put(new Element("random", "things"));
        cache.replace(new Element("key", "value"), new Element("key", "wrongValue"));
        tx1 = transactionManager.suspend();
        transactionManager.begin();
        cache.put(new Element("key", "newValue"));
        transactionManager.commit();
        transactionManager.resume(tx1);
        try {
            transactionManager.commit();
            fail("Transaction should have failed, element has changed in the meantime!");
        } catch (RollbackException e) {
            // Expected
        }
        transactionManager.begin();
        assertNotNull(cache.get("key"));
        assertEquals("newValue", cache.get("key").getValue());
        transactionManager.commit();
    }
}
