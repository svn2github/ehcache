package net.sf.ehcache.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;

import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CacheReplicator;
import net.sf.ehcache.store.TerracottaStore;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * @author twu
 * @author adahanne
 * @author cschanck
 * 
 *  orderedListeners are only accessed through package scope methods :
 *  that seems to indicate those orderedListeners were designed for a feature that never was fully implemented
 *  (in this case it was made for ingenius we suppose) -- do we get rid of this field ?
 * 
 *  there are several assertions, verifications to make when testing each of the classes (register / unresgister listeners) ;
 *  we chose to write 1 test per "behavior" :
 *  for example, when testing the registerListener methods,
 *  we wrote a test to verify notifyEventListenersChangedIfNecessary was called
 *  and a test to verify the hasReplicators was in sync with the CacheEventListener
 *
 *  testDeliverRemoteScopeAll() : can we mock an Enum, such as NotificationScope, to change one of its methods : shouldDeliver
 *  We can, unsing Powermock, that its support through IDEs does not seem to be ideal
 *  a better approach would be to use a seam, extracting listenerWrapper.getScope().shouldDeliver(remoteEvent) in a seam method
 *  and overriding it extending the class under test
 *  
 *  We ommitted the return value of registerListener and unregisterListener
 * 
 * 
 */
public class RegisteredEventListenersTest {
    private RegisteredEventListeners createRegisteredEventListeners() {
        Ehcache cache = mock(Ehcache.class);
        CacheStoreHelper cacheStoreHelper = mock(CacheStoreHelper.class);
        return new RegisteredEventListeners(cache, cacheStoreHelper);
    }

    @Test
    public void testDetectRegisterCacheReplicator() {
        RegisteredEventListeners registeredEventListeners = createRegisteredEventListeners();

        CacheEventListener listener = mock(CacheReplicator.class);
        registeredEventListeners.registerListener(listener);
        assertThat(registeredEventListeners.hasCacheReplicators(), is(true));
    }

    @Test
    public void testDetectRegisterNonCacheReplicator() {
        RegisteredEventListeners registeredEventListeners = createRegisteredEventListeners();

        CacheEventListener listener = mock(CacheEventListener.class);
        registeredEventListeners.registerListener(listener);
        assertThat(registeredEventListeners.hasCacheReplicators(), is(false));
    }

    @Test
    public void testCanRegisterListener() {
        RegisteredEventListeners registeredEventListeners = createRegisteredEventListeners();

        CacheEventListener listener = mock(CacheEventListener.class);

        boolean registerListener = registeredEventListeners.registerListener(listener);
        assertTrue(registerListener);
        assertThat(registeredEventListeners.getCacheEventListeners(), hasItem(listener));
    }

    @Test
    public void testCanUnregisterListener() throws Exception {
        RegisteredEventListeners registeredEventListeners = createRegisteredEventListeners();
        CacheEventListener listener = mock(CacheEventListener.class);

        registeredEventListeners.registerListener(listener);
        boolean unregisterListener = registeredEventListeners.unregisterListener(listener);
        assertTrue(unregisterListener);

        assertThat((Collection) registeredEventListeners.getCacheEventListeners(), (Matcher) emptyIterable());
    }

    @Test
    public void testCacheReplicatorAccounting() throws Exception {
        RegisteredEventListeners registeredEventListeners = createRegisteredEventListeners();

        CacheEventListener listener1 = mock(CacheEventListener.class);
        CacheEventListener listener2 = mock(CacheEventListener.class);
        CacheEventListener replicator1 = mock(CacheReplicator.class);
        CacheEventListener replicator2 = mock(CacheReplicator.class);

        registeredEventListeners.registerListener(listener1);
        registeredEventListeners.registerListener(listener2);
        registeredEventListeners.registerListener(replicator1);
        registeredEventListeners.registerListener(replicator2);
        assertThat(registeredEventListeners.hasCacheReplicators(), is(true));

        registeredEventListeners.unregisterListener(replicator1);
        assertThat(registeredEventListeners.hasCacheReplicators(), is(true));

        registeredEventListeners.unregisterListener(replicator2);
        assertThat(registeredEventListeners.hasCacheReplicators(), is(false));
    }

    @Test
    public void testNotifyTerracottaStoreOfListenerChangeOnRegister() throws Exception {
        Ehcache cache = mock(Ehcache.class);
        when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
        TerracottaStore store = mock(TerracottaStore.class);
        CacheStoreHelper cacheStoreHelper = mock(CacheStoreHelper.class);
        when(cacheStoreHelper.getStore()).thenReturn(store);
        RegisteredEventListeners registeredEventListeners = new RegisteredEventListeners(cache, cacheStoreHelper);

        CacheEventListener listener = mock(CacheEventListener.class);

        registeredEventListeners.registerListener(listener);
        verify(store).notifyCacheEventListenersChanged();
    }

    @Test
    public void testNotifyTerracottaStoreOfListenerChangeOnUnregister() throws Exception {
        Ehcache cache = mock(Ehcache.class);
        when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
        TerracottaStore store = mock(TerracottaStore.class);
        CacheStoreHelper cacheStoreHelper = mock(CacheStoreHelper.class);
        when(cacheStoreHelper.getStore()).thenReturn(store);
        RegisteredEventListeners registeredEventListeners = new RegisteredEventListeners(cache, cacheStoreHelper);

        CacheEventListener listener = mock(CacheEventListener.class);

        registeredEventListeners.registerListener(listener);
        registeredEventListeners.unregisterListener(listener);
        verify(store, times(2)).notifyCacheEventListenersChanged();
    }
}
