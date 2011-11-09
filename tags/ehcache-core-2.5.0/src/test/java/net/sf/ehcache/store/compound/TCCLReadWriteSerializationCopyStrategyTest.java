package net.sf.ehcache.store.compound;

import java.io.Serializable;
import java.net.URLClassLoader;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.DefaultElementValueComparator;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author teck
 */
public class TCCLReadWriteSerializationCopyStrategyTest {

    @Test
    public void test() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration().copyOnRead(true).copyOnWrite(true);
        ReadWriteCopyStrategy<Element> copyStrategy = cacheConfiguration.getCopyStrategy();
        DefaultElementValueComparator comparator = new DefaultElementValueComparator(cacheConfiguration);

        {
            // loaded via TCCL
            Element storageValue = copyStrategy.copyForWrite(new Element(1, new Foo(42)));
            Assert.assertTrue(storageValue.getObjectValue() instanceof byte[]);
            Assert.assertTrue(comparator.equals(copyStrategy.copyForWrite(new Element(1, new Foo(42))), (storageValue)));
        }

        {
            // loaded via serialization class resolve
            Thread.currentThread().setContextClassLoader(null);
            Element storageValue = copyStrategy.copyForWrite(new Element(1, new Foo(42)));
            Assert.assertTrue(storageValue.getObjectValue() instanceof byte[]);
            Assert.assertTrue(comparator.equals(copyStrategy.copyForWrite(new Element(1, new Foo(42))), (storageValue)));
        }

        {
            // Type only in TCCL
            ClassLoader loader = new Loader();
            Thread.currentThread().setContextClassLoader(loader);

            Object foo = createFooInOtherLoader(loader);
            Assert.assertEquals(loader, foo.getClass().getClassLoader());

            Element storageValue = copyStrategy.copyForWrite(new Element(1, foo));
            Assert.assertTrue(storageValue.getObjectValue() instanceof byte[]);
            Assert.assertTrue(comparator.equals(copyStrategy.copyForWrite(new Element(1, createFooInOtherLoader(loader))), (storageValue)));
        }
    }

    private Object createFooInOtherLoader(ClassLoader loader) throws Exception {
        Class c = loader.loadClass(Foo.class.getName());
        return c.getConstructor(Integer.TYPE).newInstance(42);
    }

    public static class Foo implements Serializable {

        private final int val;

        public Foo(int val) {
            this.val = val;
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

    private static class Loader extends URLClassLoader {
        public Loader() {
            super(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs(), null);
        }
    }

}
