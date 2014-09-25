package net.sf.ehcache.store.compound;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.DefaultElementValueComparator;

import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class PartialSerializationCopyStrategyTest {

    private final ClassLoader loader = getClass().getClassLoader();

    @Test
    public void test() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration().copyOnRead(true).copyOnWrite(true);
        ReadWriteCopyStrategy<Element> copyStrategy = cacheConfiguration.getCopyStrategy();
        DefaultElementValueComparator comparator = new DefaultElementValueComparator(cacheConfiguration);

        {
            Element storageValue = copyStrategy.copyForWrite(null, loader);
            // null element stays null
            Assert.assertNull(storageValue);
            Assert.assertNull(copyStrategy.copyForRead(storageValue, loader));
        }

        {
            Element storageValue = copyStrategy.copyForWrite(new Element(1, null), loader);
            // null value stays null
            Assert.assertNull(storageValue.getObjectValue());
            Assert.assertNull(copyStrategy.copyForRead(storageValue, loader).getObjectValue());
        }

        {
            Element storageValue = copyStrategy.copyForWrite(new Element(1, "one"), loader);
            // element values are stored as byte[]
            Assert.assertTrue(storageValue.getObjectValue() instanceof byte[]);
            Assert.assertTrue(comparator.equals(copyStrategy.copyForWrite(new Element(1, "one"), loader), storageValue));
        }

        {
            final short[][] value = {
                    new short[]{1},
                    new short[]{1, 2},
                    new short[]{1, 2, 3}
            };
            Element storageValue = copyStrategy.copyForWrite(new Element(1, value), loader);
            // element values are stored as byte[]
            Assert.assertTrue(storageValue.getObjectValue() instanceof byte[]);
            Assert.assertTrue(Arrays.deepEquals(new Object[]{value}, new Object[]{copyStrategy.copyForRead(storageValue, loader).getObjectValue()}));
        }

        {
            Foo foo1 = new Foo(1);
            Foo foo11 = new Foo(1);
            foo11.addExtra("extra");
            Foo foo2 = new Foo(2);

            Element storageValue = copyStrategy.copyForWrite(new Element(1, foo1), loader);
            Assert.assertTrue(storageValue.getObjectValue() instanceof byte[]);
            Assert.assertTrue(comparator.equals(copyStrategy.copyForWrite(new Element(1, foo1), loader), storageValue));
            Assert.assertFalse(comparator.equals(copyStrategy.copyForWrite(new Element(1, foo11), loader), storageValue));
//            Assert.assertTrue(comparator.equals(copyStrategy.copyForWrite(new Element(1, foo11)), storageValue));
            Assert.assertFalse(comparator.equals(copyStrategy.copyForWrite(new Element(1, foo2), loader), storageValue));
        }
    }

    public static class Foo implements Serializable {

        private final int val;
        private final Set<String> extra = new HashSet<String>();

        public Foo(int val) {
            this.val = val;
        }

        public void addExtra(String s) {
            extra.add(s);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Foo) {
                return ((Foo) obj).val == val;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return val;
        }
    }

}
