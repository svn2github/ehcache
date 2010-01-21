package net.sf.ehcache.store;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionContext;
import net.sf.ehcache.transaction.xa.EhCacheXAResourceImpl;
import net.sf.ehcache.transaction.xa.EhCacheXAStoreImpl;
import net.sf.ehcache.transaction.xa.XaTransactionContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import java.util.Arrays;

/**
 * @author Alex Snaps
 */
public class XaIsolationTransactionalStoreTest {

    private static final String KEY       = "KEY";
    private static final String OTHER_KEY = "OTHER";

    private Store                 store;
    private Store                 underlyingStore;
    private EhCacheXAResourceImpl xaResource;

    private boolean keyInStore      = false;

    @Before
    public void setupMockedStore()
            throws SystemException, RollbackException {
        
        EhCacheXAResourceImpl xaResource = mock(EhCacheXAResourceImpl.class);
        underlyingStore = mock(Store.class);
        Transaction tx = mock(Transaction.class);
        Xid xid = mock(Xid.class);
        this.xaResource = xaResource;
        when(this.xaResource.getStore()).thenReturn(underlyingStore);
        TransactionContext txContext = new XaTransactionContext(xid, new EhCacheXAStoreImpl(underlyingStore, mock(Store.class)));
        when(xaResource.getOrCreateTransactionContext()).thenReturn(txContext);
        when(underlyingStore.isCacheCoherent()).thenReturn(true);
        when(underlyingStore.getKeyArray()).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                if(keyInStore) {
                    return new Object[] { KEY };
                }
                return new Object[0];
            }
        });
        when(underlyingStore.containsKey(KEY)).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                return keyInStore;
            }
        });
        when(underlyingStore.containsKey(OTHER_KEY)).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock)
                    throws Throwable {
                return false;
            }
        });
        store = new XaTransactionalStore(xaResource);
    }

    @Test
    public void testIsolationNoKey() {
        Element element = new Element(KEY, "VALUE");
        assertThat(store.get(element.getKey()), nullValue());
        assertThat(store.getSize(), is(0));
        assertThat(store.containsKey(KEY), is(false));
        store.put(element);
        assertThat(store.get(element.getKey()), sameInstance(element));
        assertThat(store.getSize(), is(1));
        assertThat(store.containsKey(KEY), is(true));
        assertThat(store.remove(element.getKey()), sameInstance(element));
        assertThat(store.getSize(), is(0));
        assertThat(store.containsKey(KEY), is(false));
        assertThat(store.get(element.getKey()), nullValue());
        assertThat(store.remove(element.getKey()), nullValue());
        assertThat(store.getSize(), is(0));
        store.put(element);
        store.put(element);
        assertThat(store.get(element.getKey()), sameInstance(element));
        assertThat(store.getSize(), is(1));
        assertThat(store.containsKey(KEY), is(true));
        assertThat(store.remove(KEY), sameInstance(element));
        assertThat(store.remove(KEY), nullValue());
        store.remove(KEY);
        assertThat(store.getSize(), is(0));
        assertThat(store.get(element.getKey()), nullValue());

        assertThat(store.containsKey(KEY), is(false));
    }

    @Test
    public void testIsolationWithKey() {
        Element element = new Element(KEY, "VALUE");
        when(xaResource.get(element.getKey())).thenReturn(element);
        when(underlyingStore.getSize()).thenReturn(1);
        keyInStore = true;
        assertThat(store.get(element.getKey()), sameInstance(element));
        assertThat(store.getSize(), is(1));
        assertThat(store.getKeyArray().length, is(1));
        assertThat(store.containsKey(KEY), is(true));
        Element newElement = new Element(element.getKey(), "NEW_VALUE");
        store.put(newElement);
        assertThat(store.get(element.getKey()), sameInstance(newElement));
        assertThat(store.getSize(), is(1));
        assertThat(store.getKeyArray().length, is(1));
        assertThat(store.containsKey(KEY), is(true));
        assertThat(store.remove(element.getKey()), sameInstance(newElement));
        assertThat(store.getSize(), is(0));
        assertThat(store.getKeyArray().length, is(0));
        assertThat(store.get(element.getKey()), nullValue());
        assertThat(store.remove(element.getKey()), nullValue());
        assertThat(store.getSize(), is(0));
        assertThat(store.getKeyArray().length, is(0));
        assertThat(store.containsKey(KEY), is(false));
        store.put(newElement);
        store.put(newElement);
        assertThat(store.get(element.getKey()), sameInstance(newElement));
        assertThat(store.getSize(), is(1));
        assertThat(store.getKeyArray().length, is(1));
        assertThat(store.containsKey(KEY), is(true));
        assertThat(store.remove(KEY), sameInstance(newElement));
        assertThat(store.remove(KEY), nullValue());
        assertThat(store.getSize(), is(0));
        assertThat(store.getKeyArray().length, is(0));
        assertThat(store.get(element.getKey()), nullValue());
        assertThat(store.containsKey(KEY), is(false));
    }

    @Test
    public void testIsolationWithOtherKey() {
        Element element = new Element(KEY, "VALUE");
        when(xaResource.get(element.getKey())).thenReturn(element);
        when(xaResource.getQuiet(element.getKey())).thenReturn(element);
        when(underlyingStore.getSize()).thenReturn(1);
        keyInStore = true;

        assertThat(store.get(element.getKey()), sameInstance(element));
        assertThat(store.getSize(), is(1));
        assertThat(store.getKeyArray().length, is(1));
        assertThat(store.containsKey(KEY), is(true));
        assertThat(store.containsKey(OTHER_KEY), is(false));
        Element newElement = new Element(OTHER_KEY, "NEW_VALUE");
        store.put(newElement);
        assertThat(store.get(element.getKey()), sameInstance(element));
        assertThat(store.get(KEY), sameInstance(element));
        assertThat(store.get(OTHER_KEY), sameInstance(newElement));
        assertThat(store.getSize(), is(2));
        assertThat(store.getKeyArray().length, is(2));
        assertThat(store.containsKey(element.getKey()), is(true));
        assertThat(store.containsKey(KEY), is(true));
        assertThat(store.remove(OTHER_KEY), sameInstance(newElement));
        assertThat(store.getSize(), is(1));
        assertThat(store.getKeyArray().length, is(1));
        assertThat(store.get(OTHER_KEY), nullValue());
        assertThat(store.get(KEY), sameInstance(element));
        assertThat(store.remove(OTHER_KEY), nullValue());
        assertThat(store.getSize(), is(1));
        assertThat(store.getKeyArray().length, is(1));
        assertThat(store.containsKey(OTHER_KEY), is(false));
        assertThat(store.containsKey(KEY), is(true));
        store.put(newElement);
        store.put(newElement);
        assertThat(store.get(OTHER_KEY), sameInstance(newElement));
        assertThat(store.getSize(), is(2));
        assertThat(store.getKeyArray().length, is(2));
        assertThat(store.containsKey(KEY), is(true));
        assertThat(store.containsKey(OTHER_KEY), is(true));
        assertThat(store.remove(OTHER_KEY), sameInstance(newElement));
        assertThat(store.remove(OTHER_KEY), nullValue());
        assertThat(store.remove(KEY), sameInstance(element));
        assertThat(store.remove(KEY), nullValue());
        assertThat(store.getSize(), is(0));
        assertThat(store.getKeyArray().length, is(0));
        assertThat(store.get(OTHER_KEY), nullValue());
        assertThat(store.get(KEY), nullValue());
        assertThat(store.containsKey(OTHER_KEY), is(false));
        assertThat(store.containsKey(KEY), is(false));
    }

}
