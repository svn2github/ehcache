package net.sf.ehcache.concurrent;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class StripedReadWriteLockSyncTest {
    private Object[] keys;
    private CacheLockProvider clp;

    @Before
    public void setUp() throws Exception {
        keys = new Object[]{new StupidKey(0), new StupidKey(0)};
        clp = new StripedReadWriteLockSync(2);
    }

    @Test
    public void testIsProperlyReentrant() {

        Sync syncForKey = clp.getSyncForKey(keys[0]);

        assertSame(syncForKey, clp.getSyncForKey(keys[1]));

        clp.getAndWriteLockAllSyncForKeys(keys);
        for (Object key : keys) {
            assertTrue(syncForKey.isHeldByCurrentThread(LockType.WRITE));
            clp.getSyncForKey(key).unlock(LockType.WRITE);
            if (key != keys[keys.length - 1]) {
                assertTrue(syncForKey.isHeldByCurrentThread(LockType.WRITE));
            }
        }

        assertFalse(syncForKey.isHeldByCurrentThread(LockType.WRITE));
    }

    @Test
    public void testIsProperlyUnlockingAllKeys() {
        Sync syncForKey = clp.getSyncForKey(keys[0]);

        assertSame(syncForKey, clp.getSyncForKey(keys[1]));

        clp.getAndWriteLockAllSyncForKeys(keys);
        clp.unlockWriteLockForAllKeys(keys);

        assertFalse(syncForKey.isHeldByCurrentThread(LockType.WRITE));
    }

    private static final class StupidKey {
        private final int hashCode;

        private StupidKey(final int hashCode) {
            this.hashCode = hashCode;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
