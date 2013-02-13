package net.sf.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.pool.sizeof.AgentSizeOf;
import net.sf.ehcache.pool.sizeof.ReflectionSizeOf;
import net.sf.ehcache.pool.sizeof.SizeOf;
import net.sf.ehcache.pool.sizeof.UnsafeSizeOf;
import net.sf.ehcache.store.Store;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.matchers.EqualsWithDelta;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class PoolCacheManagerTest {

    private static long deepSizeOf(SizeOf sizeOf, Object... obj) {
        return sizeOf.deepSizeOf(1000, true, obj).getCalculated();
    }
    
  
    @Before
    public void setup() {
        getSizeOfEngine().sizeOf("");
        System.err.println("Testing for a " + System.getProperty("java.version") + " JDK on a "
                           + System.getProperty("sun.arch.data.model") + "bit VM");
    }

    @Ignore
    @Test
    public void testOnHeapConsumption() throws Exception {
        SizeOf sizeOf = getSizeOfEngine();
        CacheManager cacheManager = new CacheManager(new Configuration().maxBytesLocalHeap(40, MemoryUnit.MEGABYTES));
        cacheManager.addCache(new Cache(new CacheConfiguration("one", 0).overflowToDisk(false)));
        cacheManager.addCache(new Cache(new CacheConfiguration("double", 0).overflowToDisk(false)));

        Cache oneSize = cacheManager.getCache("one");
        Cache doubleSize = cacheManager.getCache("double");

        Element test = new Element("test", new Pair("0", new Object()));
        oneSize.put(test);
        doubleSize.put(test);
        deepSizeOf(sizeOf, test);
        oneSize.remove(test.getKey());
        int size = 60000;
        for (int i = 0; i < size; i++) {
//            oneSize.put(new Element(i, new Pair(new Object(), new Object())));
//            doubleSize.put(new Element(i, new Pair(new Object(), new Object[] {new Object(), new Object()})));
//            doubleSize.put(new Element(i + size, new Pair(new Object(), new Object[] {new Object(), new Object()})));
            doubleSize.put(new Element(new Object(), new Object()));
//          doubleSize.put(new Element(i, i + "" + i));
        }
        doubleSize.removeAll();

        long usedBefore = measureMemoryUse();

        for (int i = 0; i < size; i++) {
//            oneSize.put(new Element(i, new Pair(new Object(), new Object())));
//            doubleSize.put(new Element(i, new Pair(new Object(), new Object[] {new Object(), new Object()})));
//            doubleSize.put(new Element(i + size, new Pair(new Object(), new Object[] {new Object(), new Object()})));
            oneSize.put(new Element(new Object(), new Object()));
//          doubleSize.put(new Element(i, i + "" + i));
        }

        long mem = 0;
        for (Object key : oneSize.getKeys()) {
            Element element = oneSize.get(key);
            mem += deepSizeOf(sizeOf, element);
        }
        for (Object key : doubleSize.getKeys()) {
            Element element = doubleSize.get(key);
            mem += deepSizeOf(sizeOf, element);
        }

        assertThat(MemoryUnit.MEGABYTES.toBytes(40) - mem >= 0, is(true));
        long consumes = measureMemoryUse() - usedBefore;
        assertThat(consumes +" bytes are actually being used, while we believe " + mem + " are",
            mem / (float)consumes, new EqualsWithDelta(1f, 0.025f));
    }

    private long getInMemorySizeInBytes(final Cache oneSize) throws Exception {
        Field store = Cache.class.getDeclaredField("compoundStore");
        store.setAccessible(true);
        return ((Store) store.get(oneSize)).getInMemorySizeInBytes();
    }

    protected long measureMemoryUse() throws InterruptedException {
        System.gc();
        Thread.sleep(2000);
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private static final class Pair {
        private static final AtomicLong counter = new AtomicLong(Long.MIN_VALUE);
        private final Object one;
        private final Object two;
        private final Object oneHidden;
        private final Object twoHidden;
        private final Object threeHidden;
        private final Object fourHidden;
        private final long instanceNumber;

        private Pair(final Object one, final Object two) {
            this.one = one;
            this.two = two;
            instanceNumber = counter.getAndIncrement();
            if(instanceNumber % 4 == 1) {
                oneHidden = new Object();
                twoHidden = new Object();
                threeHidden = new Object();
                fourHidden = new Object();
            } else {
                oneHidden = null;
                twoHidden = null;
                threeHidden = null;
                fourHidden = null;
            }
        }
    }

    private static SizeOf getSizeOfEngine() {
        try {
            return new AgentSizeOf();
        } catch (UnsupportedOperationException e) {
            try {
                return new UnsafeSizeOf();
            } catch (UnsupportedOperationException f) {
                try {
                    return new ReflectionSizeOf();
                } catch (UnsupportedOperationException g) {
                    throw new CacheException("A suitable SizeOf engine could not be loaded: " + e + ", " + f + ", " + g);
                }
            }
        }
    }
}
