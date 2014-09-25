package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CopyStrategyConfiguration;
import net.sf.ehcache.store.compound.ReadWriteSerializationCopyStrategy;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class TxCopyingCacheStoreTest {

    public static byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @Test
    public void testWrappingElementValueComparatorEquals() throws Exception {
        CacheConfiguration cacheConfiguration = mock(CacheConfiguration.class);
        CopyStrategyConfiguration copyStrategyConfiguration = mock(CopyStrategyConfiguration.class);

        when(copyStrategyConfiguration.getCopyStrategyInstance(any(ClassLoader.class))).thenReturn(new ReadWriteSerializationCopyStrategy());
        when(cacheConfiguration.getCopyStrategyConfiguration()).thenReturn(copyStrategyConfiguration);
        when(cacheConfiguration.isCopyOnRead()).thenReturn(true);
        when(cacheConfiguration.isCopyOnWrite()).thenReturn(true);
        when(cacheConfiguration.getClassLoader()).thenReturn(getClass().getClassLoader());

        ElementValueComparator wrappedComparator = TxCopyingCacheStore.wrap(new DefaultElementValueComparator(cacheConfiguration), cacheConfiguration);

        Element e = new Element(1, serialize("aaa"));
        assertThat(wrappedComparator.equals(e, e), is(true));
        assertThat(wrappedComparator.equals(null, null), is(true));
        assertThat(wrappedComparator.equals(null, e), is(false));
        assertThat(wrappedComparator.equals(e, null), is(false));
    }
}
