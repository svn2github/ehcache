package net.sf.ehcache.pool;

import java.io.IOException;

import java.util.Random;

import junit.framework.Assert;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;

import org.junit.After;
import org.junit.Test;

public class TwinCachesTest {

    private CacheManager manager;

    //@Test
    public void testParallelLoadTwinCaches() {
        manager = new CacheManager(new Configuration().maxBytesLocalHeap(16, MemoryUnit.MEGABYTES).defaultCache(new CacheConfiguration("default", 0).eternal(true)));

        Ehcache one = manager.addCacheIfAbsent("one");
        Ehcache two = manager.addCacheIfAbsent("two");

        int terminal = 0;
        for (int i = 0; true; i++) {
            int sizeOne = one.getSize();
            int sizeTwo = two.getSize();
            one.put(new Element(Integer.toString(i), new byte[1024]));
            two.put(new Element(Integer.toString(i), new byte[1024]));
            if (sizeOne >= one.getSize() || sizeTwo >= two.getSize()) {
                terminal = i;
                break;
            }
        }

        for (int i = terminal + 1; i < terminal + 100; i++) {
            one.put(new Element(Integer.toString(i), new byte[1024]));
            two.put(new Element(Integer.toString(i), new byte[1024]));
        }

        float ratio = 0.5f;

        long total = one.getSize() + two.getSize();

        System.err.println("[1] Ratio    : " + ratio);
        System.err.println("[1] Measured : " + (((float) one.getSize()) / total) + " [" + one.getSize() + "]");
        System.err.println("[2] Ratio    : " + (1 - ratio));
        System.err.println("[2] Measured : " + (((float) two.getSize()) / total) + " [" + two.getSize() + "]");

        Assert.assertEquals(ratio, ((float) one.getSize()) / total, 0.1f);
        Assert.assertEquals(1f - ratio, ((float)two.getSize()) / total, 0.1f);
    }

    //@Test
    public void testSerialLoadTwinCaches() {
        manager = new CacheManager(new Configuration().maxBytesLocalHeap(16, MemoryUnit.MEGABYTES).defaultCache(new CacheConfiguration("default", 0).eternal(true)));

        Ehcache one = manager.addCacheIfAbsent("one");
        Ehcache two = manager.addCacheIfAbsent("two");

        int terminal = 0;
        for (int i = 0; true; i++) {
            int sizeOne = one.getSize();
            one.put(new Element(Integer.toString(i), new byte[1024]));
            if (sizeOne >= one.getSize()) {
                terminal = i + 1;
                break;
            }
        }

        for (int i = 0; i < terminal; i++) {
            two.put(new Element(Integer.toString(i), new byte[1024]));
        }

        for (int i = terminal; i < terminal + 100; i++) {
            one.put(new Element(Integer.toString(i), new byte[1024]));
            two.put(new Element(Integer.toString(i), new byte[1024]));
        }

        float ratio = 0.5f;

        long total = one.getSize() + two.getSize();

        System.err.println("[1] Ratio    : " + ratio);
        System.err.println("[1] Measured : " + (((float) one.getSize()) / total) + " [" + one.getSize() + "]");
        System.err.println("[2] Ratio    : " + (1 - ratio));
        System.err.println("[2] Measured : " + (((float) two.getSize()) / total) + " [" + two.getSize() + "]");

        Assert.assertEquals(ratio, ((float) one.getSize()) / total, 0.1f);
        Assert.assertEquals(1f - ratio, ((float)two.getSize()) / total, 0.1f);
    }

    //@Test
    public void testRandomAccessTwinCaches() {
        manager = new CacheManager(new Configuration().maxBytesLocalHeap(1, MemoryUnit.MEGABYTES).defaultCache(new CacheConfiguration("default", 0).eternal(true)));

        Ehcache one = manager.addCacheIfAbsent("one");
        Ehcache two = manager.addCacheIfAbsent("two");

        long seed = System.nanoTime();
        System.err.println("TwinCachesTest.testRandomAccessTwinCaches seed=" + seed);
        Random rndm = new Random(seed);
        float ratio = rndm.nextFloat();

        final int MAX = 1024 * 16;
        for (int i = 0; i < 20 * MAX; i++) {
            Ehcache chosen = (rndm.nextFloat() < ratio) ? one : two;

            int key = getRandomKey(rndm, MAX);
            Element e = chosen.get(key);
            if (e == null) {
                chosen.put(new Element(key, new byte[128]));
            }
        }

        long total = one.getSize() + two.getSize();

        System.err.println("[1] Ratio    : " + ratio);
        System.err.println("[1] Measured : " + (((float) one.getSize()) / total) + " [" + one.getSize() + "]");
        System.err.println("[2] Ratio    : " + (1 - ratio));
        System.err.println("[2] Measured : " + (((float) two.getSize()) / total) + " [" + two.getSize() + "]");

        Assert.assertEquals(ratio, ((float) one.getSize()) / total, 0.1f);
        Assert.assertEquals(1f - ratio, ((float)two.getSize()) / total, 0.1f);
    }

    //@Test
    public void testRandomAccessTripletCaches() {
        manager = new CacheManager(new Configuration().maxBytesLocalHeap(1, MemoryUnit.MEGABYTES).defaultCache(new CacheConfiguration("default", 0).eternal(true)));

        Ehcache one = manager.addCacheIfAbsent("one");
        Ehcache two = manager.addCacheIfAbsent("two");
        Ehcache three = manager.addCacheIfAbsent("three");

        long seed = System.nanoTime();
        System.err.println("TwinCachesTest.testRandomAccessTripletCaches seed=" + seed);
        Random rndm = new Random(seed);
        float ratioOne = rndm.nextFloat();
        float ratioTwo = (rndm.nextFloat() * (1 - ratioOne)) + ratioOne;
        final int MAX = 1024 * 16;
        for (int i = 0; i < 20 * MAX; i++) {
            Ehcache chosen;
            float choice = rndm.nextFloat();
            if (choice < ratioOne) {
                chosen = one;
            } else if (choice < ratioTwo) {
                chosen = two;
            } else {
                chosen = three;
            }

            int key = getRandomKey(rndm, MAX);
            Element e = chosen.get(key);
            if (e == null) {
                chosen.put(new Element(key, new byte[128]));
            }
        }

        long total = one.getSize() + two.getSize() + three.getSize();

        System.err.println("[1] Ratio    : " + ratioOne);
        System.err.println("[1] Measured : " + (((float) one.getSize()) / total) + " [" + one.getSize() + "]");
        System.err.println("[2] Ratio    : " + (ratioTwo - ratioOne));
        System.err.println("[2] Measured : " + (((float) two.getSize()) / total) + " [" + two.getSize() + "]");
        System.err.println("[3] Ratio    : " + (1 - ratioTwo));
        System.err.println("[3] Measured : " + (((float) three.getSize()) / total) + " [" + three.getSize() + "]");

        Assert.assertEquals(ratioOne, ((float) one.getSize()) / total, 0.1f);
        Assert.assertEquals(ratioTwo - ratioOne, ((float) two.getSize()) / total, 0.1f);
        Assert.assertEquals(1 - ratioTwo, ((float)three.getSize()) / total, 0.1f);
    }

    //@Test
    public void testIntroducedRandomAccessTwinCache() throws IOException {
        manager = new CacheManager(new Configuration().maxBytesLocalHeap(2, MemoryUnit.MEGABYTES).defaultCache(new CacheConfiguration("default", 0).eternal(true)));

        Ehcache one = manager.addCacheIfAbsent("one");

        long seed = System.nanoTime();
        System.err.println("TwinCachesTest.testIntroducedRandomAccessTwinCache seed=" + seed);
        Random rndm = new Random(seed);
        float ratio = rndm.nextFloat();

        final int MAX = 1024 * 32;

        for (int key = 0; key < MAX; key++) {
            int size = one.getSize();
            one.put(new Element(key, new byte[128]));
            if (size >= one.getSize()) {
                break;
            }
        }

        Ehcache two = manager.addCacheIfAbsent("two");

        for (int i = 0; i < 20 * MAX; i++) {
            Ehcache chosen = (rndm.nextFloat() < ratio) ? one : two;

            int key = getRandomKey(rndm, MAX);
            Element e = chosen.get(key);
            if (e == null) {
                chosen.put(new Element(key, new byte[128]));
            }
        }

        long total = one.getSize() + two.getSize();

        System.err.println("[1] Ratio    : " + ratio);
        System.err.println("[1] Measured : " + (((float) one.getSize()) / total) + " [" + one.getSize() + "]");
        System.err.println("[2] Ratio    : " + (1 - ratio));
        System.err.println("[2] Measured : " + (((float) two.getSize()) / total) + " [" + two.getSize() + "]");

        Assert.assertEquals(ratio, ((float) one.getSize()) / total, 0.1f);
        Assert.assertEquals(1f - ratio, ((float)two.getSize()) / total, 0.1f);
    }

    @Test
    public void testIntroducedRandomAccessTripletCache() throws IOException {
        manager = new CacheManager(new Configuration().maxBytesLocalHeap(2, MemoryUnit.MEGABYTES).defaultCache(new CacheConfiguration("default", 0).eternal(true)));

        Ehcache one = manager.addCacheIfAbsent("one");

        final int MAX = 1024 * 32;

        for (int key = 0; key < MAX; key++) {
            int size = one.getSize();
            one.put(new Element(key, new byte[128]));
            if (size >= one.getSize()) {
                break;
            }
        }

        long seed = System.nanoTime();
        System.err.println("TwinCachesTest.testIntroducedRandomAccessTripletCache seed=" + seed);
        Random rndm = new Random(seed);
        float ratio = rndm.nextFloat();

        Ehcache two = manager.addCacheIfAbsent("two");

        for (int i = 0; i < 20 * MAX; i++) {
            Ehcache chosen = (rndm.nextFloat() < ratio) ? one : two;

            int key = getRandomKey(rndm, MAX);
            Element e = chosen.get(key);
            if (e == null) {
                chosen.put(new Element(key, new byte[128]));
            }
        }

        long totalTwo = one.getSize() + two.getSize();

        System.err.println("[1] Ratio    : " + ratio);
        System.err.println("[1] Measured : " + (((float) one.getSize()) / totalTwo) + " [" + one.getSize() + "]");
        System.err.println("[2] Ratio    : " + (1 - ratio));
        System.err.println("[2] Measured : " + (((float) two.getSize()) / totalTwo) + " [" + two.getSize() + "]");

        Assert.assertEquals(ratio, ((float) one.getSize()) / totalTwo, 0.1f);
        Assert.assertEquals(1f - ratio, ((float) two.getSize()) / totalTwo, 0.1f);

        float ratioOne = rndm.nextFloat();
        float ratioTwo = (rndm.nextFloat() * (1 - ratioOne)) + ratioOne;

        Ehcache three = manager.addCacheIfAbsent("three");

        for (int i = 0; i < 20 * MAX; i++) {
            Ehcache chosen;
            float choice = rndm.nextFloat();
            if (choice < ratioOne) {
                chosen = one;
            } else if (choice < ratioTwo) {
                chosen = two;
            } else {
                chosen = three;
            }

            int key = getRandomKey(rndm, MAX);
            Element e = chosen.get(key);
            if (e == null) {
                chosen.put(new Element(key, new byte[128]));
            }
        }

        long totalThree = one.getSize() + two.getSize() + three.getSize();

        System.err.println("[1] Ratio    : " + ratioOne);
        System.err.println("[1] Measured : " + (((float) one.getSize()) / totalThree) + " [" + one.getSize() + "]");
        System.err.println("[2] Ratio    : " + (ratioTwo - ratioOne));
        System.err.println("[2] Measured : " + (((float) two.getSize()) / totalThree) + " [" + two.getSize() + "]");
        System.err.println("[3] Ratio    : " + (1 - ratioTwo));
        System.err.println("[3] Measured : " + (((float) three.getSize()) / totalThree) + " [" + three.getSize() + "]");

        //((GraphingPoolEvictor) manager.getOnHeapPool().getEvictor()).dumpGraph(new File("testIntroducedRandomAccessTripletCache.png"));
        Assert.assertEquals(ratioOne, ((float) one.getSize()) / totalThree, 0.1f);
        Assert.assertEquals(ratioTwo - ratioOne, ((float) two.getSize()) / totalThree, 0.1f);
        Assert.assertEquals(1 - ratioTwo, ((float)three.getSize()) / totalThree, 0.1f);
    }

    //@Test
    public void testIntroducedRandomAccessDoubledCache() throws IOException {
        doTestIntroducedAccessDoubledCache(System.nanoTime());
    }

    //@Test
    public void testIntroducedFixedAccessDoubledCache() throws IOException {
        // See MNK-3643
        doTestIntroducedAccessDoubledCache(944752613893346L);
    }

    private void doTestIntroducedAccessDoubledCache(final long seed) {
        manager = new CacheManager(new Configuration().maxBytesLocalHeap(2, MemoryUnit.MEGABYTES).defaultCache(new CacheConfiguration("default", 0).eternal(true)));

        Ehcache one = manager.addCacheIfAbsent("one");

        System.err.println("TwinCachesTest.testIntroducedRandomAccessDoubledCache seed=" + seed);
        Random rndm = new Random(seed);
        float ratio = rndm.nextFloat();

        final int MAX = 1024 * 32;

        for (int key = 0; key < MAX; key++) {
            int size = one.getSize();
            one.put(new Element(key, new byte[128]));
            if (size >= one.getSize()) {
                break;
            }
        }

        Ehcache two = manager.addCacheIfAbsent("two");

        for (int i = 0; i < 20 * MAX; i++) {
            if (rndm.nextFloat() < ratio) {
                int key = getRandomKey(rndm, MAX);
                Element e = one.get(key);
                if (e == null) {
                    one.put(new Element(key, new byte[128]));
                }
            } else {
                int key = getRandomKey(rndm, MAX);
                Element e = two.get(key);
                if (e == null) {
                    two.put(new Element(key, new byte[256]));
                }
            }
        }

        long totalCount = one.getSize() + two.getSize();

        System.err.println("[1] Count Ratio    : " + ratio);
        System.err.println("[1] Count Measured : " + (((float) one.getSize()) / totalCount) + " [" + one.getSize() + "]");
        System.err.println("[2] Count Ratio    : " + (1 - ratio));
        System.err.println("[2] Count Measured : " + (((float) two.getSize()) / totalCount) + " [" + two.getSize() + "]");

        Assert.assertEquals(ratio, ((float)one.getSize()) / totalCount, 0.1f);
        Assert.assertEquals(1f - ratio, ((float) two.getSize()) / totalCount, 0.1f);

        long totalBytes = one.getStatistics().getLocalHeapSizeInBytes() +
                two.getStatistics().getLocalHeapSizeInBytes();
        float bytesRatio = (ratio * 128f) / ((ratio * 128f) + ((1 - ratio) * 256f));

        System.err.println("[1] Bytes Ratio    : " + bytesRatio);
        System.err.println("[1] Bytes Measured : " + (((float) one.getStatistics().getLocalHeapSizeInBytes()) / totalBytes) + " ["
                + one.getStatistics().getLocalHeapSizeInBytes() + "]");
        System.err.println("[2] Bytes Ratio    : " + (1 - bytesRatio));
        System.err.println("[2] Bytes Measured : " + (((float) two.getStatistics().getLocalHeapSizeInBytes()) / totalBytes) + " ["
                + two.getStatistics().getLocalHeapSizeInBytes() + "]");

        Assert.assertEquals(bytesRatio, ((float) one.getStatistics().getLocalHeapSizeInBytes()) / totalBytes, 0.1f);
        Assert.assertEquals(1f - bytesRatio, ((float)two.getStatistics().getLocalHeapSizeInBytes()) / totalBytes, 0.1f);
    }

    private static int getRandomKey(Random rndm, int max) {
        int key;
        do {
            key = (int) (((rndm.nextGaussian() + 10.0f) / 20.0f) * max);
        } while (key < 0 || key >= max);
        return key;
    }

    @After
    public void tearDown() {
        if(manager != null && manager.getStatus() == Status.STATUS_ALIVE) {
            manager.shutdown();
        }
    }
}
