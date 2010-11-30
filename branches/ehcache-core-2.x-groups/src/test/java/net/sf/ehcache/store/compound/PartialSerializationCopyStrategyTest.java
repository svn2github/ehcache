package net.sf.ehcache.store.compound;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.DefaultElementValueComparator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Ludovic Orban
 */
public class PartialSerializationCopyStrategyTest {

    @Test
    public void test() throws Exception {
        final DefaultElementValueComparator comparator = new DefaultElementValueComparator();
        final ReadWriteSerializationCopyStrategy copyStrategy = new ReadWriteSerializationCopyStrategy();

        {
            Element storageValue = copyStrategy.copyForWrite(null);
            // null element stays null
            Assert.assertNull(storageValue);
            Assert.assertNull(copyStrategy.copyForRead(storageValue));
        }

        {
            Element storageValue = copyStrategy.copyForWrite(new Element(1, null));
            // null value stays null
            Assert.assertNull(storageValue.getObjectValue());
            Assert.assertNull(copyStrategy.copyForRead(storageValue).getObjectValue());
        }

        {
            Element storageValue = copyStrategy.copyForWrite(new Element(1, "one"));
            // element values are stored as byte[]
            Assert.assertTrue(storageValue.getObjectValue() instanceof byte[]);
            Assert.assertTrue(comparator.equals(new Element(1, "one"), copyStrategy.copyForRead(storageValue)));
        }

        {
            final short[][] value = {
                    new short[]{1},
                    new short[]{1, 2},
                    new short[]{1, 2, 3}
            };
            Element storageValue = copyStrategy.copyForWrite(new Element(1, value));
            // element values are stored as byte[]
            Assert.assertTrue(storageValue.getObjectValue() instanceof byte[]);
            Assert.assertTrue(Arrays.deepEquals(new Object[] {value}, new Object[] {copyStrategy.copyForRead(storageValue).getObjectValue()}));
        }

    }

}
