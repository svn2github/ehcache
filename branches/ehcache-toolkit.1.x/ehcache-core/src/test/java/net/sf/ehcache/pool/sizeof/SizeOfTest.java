package net.sf.ehcache.pool.sizeof;

import static net.sf.ehcache.pool.sizeof.JvmInformation.CURRENT_JVM_INFORMATION;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.Proxy;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import junit.framework.Assert;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class SizeOfTest extends AbstractSizeOfTest {

  public Object[] container;

  private static long deepSizeOf(SizeOf sizeOf, Object... obj) {
    return sizeOf.deepSizeOf(1000, true, obj).getCalculated();
  }

  @BeforeClass
  public static void setup() {
    deepSizeOf(new CrossCheckingSizeOf(), new EvilPair(new Object(), new SomeClass(true)));
    System.err.println("Testing for a " + System.getProperty("java.version") + " JDK on a "
                       + System.getProperty("sun.arch.data.model") + "bit VM (compressed-oops: " + COMPRESSED_OOPS + ")");
  }

  @Test
  public void testSizeOf() throws Exception {
    Assume.assumeThat(CURRENT_JVM_INFORMATION.getMinimumObjectSize(), is(CURRENT_JVM_INFORMATION.getObjectAlignment()));

    SizeOf sizeOf = new CrossCheckingSizeOf();
    String version = System.getProperty("java.version");
    if (version.startsWith("1.5")) {
      if (IS_64_BIT) {
        verify64bitSizes(sizeOf);
        assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(136L));
      } else {
        verify32bitSizes(sizeOf);
        assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(80L));
      }
    } else if (version.startsWith("1.6")) {
      if (IS_64_BIT) {
        if (IS_JROCKIT) {
          verify64bitJRockit4GBCompressedRefsSizes(sizeOf);
          assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(144L));
        } else if (IS_IBM) {
            verify64bitIBMSizes(sizeOf);
            assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(224L));
        } else if (COMPRESSED_OOPS) {
          verify64bitCompressedOopsSizes(sizeOf);
          assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(112L));
        } else {
          verify64bitSizes(sizeOf);
          assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(176L));
        }
      } else if (IS_IBM) {
          verify32bitIBMSizes(sizeOf);
          assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(112L));
      } else if (IS_JROCKIT) {
          verify32bitJRockitSizes(sizeOf);
          assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(144L));
      } else {
        verify32bitSizes(sizeOf);
        assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(104L));
      }
    } else if (version.startsWith("1.7")) {
      if (IS_64_BIT) {
        if (COMPRESSED_OOPS) {
          verify64bitCompressedOopsSizes(sizeOf);
          assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(120L));
        } else {
          verify64bitSizes(sizeOf);
          assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(192L));
        }
      } else {
        verify32bitSizes(sizeOf);
        assertThat(deepSizeOf(sizeOf, new ReentrantReadWriteLock()), is(112L));
      }        
    } else {
        Assert.fail();
    }

    List<Object> list1 = new ArrayList<Object>();
    List<Object> list2 = new ArrayList<Object>();

    Object someInstance = new Object();
    list1.add(someInstance);
    list2.add(someInstance);

    assertThat(deepSizeOf(sizeOf, list1), is(deepSizeOf(sizeOf, list2)));
    assertThat(deepSizeOf(sizeOf, list1, list2) < (deepSizeOf(sizeOf, list1) + deepSizeOf(sizeOf, list2)), is(true));
    list2.add(new Object());
    assertThat(deepSizeOf(sizeOf, list2) > deepSizeOf(sizeOf, list1), is(true));
  }

  private void verify32bitJRockitSizes(SizeOf sizeOf) {
    verifyFlyweights(sizeOf);
    assertThat(sizeOf.sizeOf(new Object()), is(16L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(24L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(deepSizeOf(sizeOf, new Integer(1))));
    assertThat(sizeOf.sizeOf(1000), is(24L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(false)), is(24L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(true)), is(40L));
    assertThat(sizeOf.sizeOf(new Object[] { }), is(24L));
    assertThat(sizeOf.sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() }), is(40L));
    assertThat(sizeOf.sizeOf(new int[] { }), is(24L));
    assertThat(sizeOf.sizeOf(new int[] { 987654, 876543, 765432, 654321 }), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(null, null)), is(24L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), null)), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), new Object())), is(56L));
  }

  private void verify32bitSizes(SizeOf sizeOf) {
    verifyFlyweights(sizeOf);
    assertThat(sizeOf.sizeOf(new Object()), is(8L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(16L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(deepSizeOf(sizeOf, new Integer(1))));
    assertThat(sizeOf.sizeOf(1000), is(16L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(false)), is(16L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(true)), is(24L));
    assertThat(sizeOf.sizeOf(new Object[] { }), is(16L));
    assertThat(sizeOf.sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() }), is(32L));
    assertThat(sizeOf.sizeOf(new int[] { }), is(16L));
    assertThat(sizeOf.sizeOf(new int[] { 987654, 876543, 765432, 654321 }), is(32L));
    assertThat(deepSizeOf(sizeOf, new Pair(null, null)), is(16L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), null)), is(24L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), new Object())), is(32L));
  }

  private void verify64bitSizes(SizeOf sizeOf) {
    verifyFlyweights(sizeOf);
    assertThat(sizeOf.sizeOf(new Object()), is(16L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(24L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(deepSizeOf(sizeOf, new Integer(1))));
    assertThat(sizeOf.sizeOf(1000), is(24L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(false)), is(24L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(true)), is(40L));
    assertThat(sizeOf.sizeOf(new Object[] { }), is(24L));
    assertThat(sizeOf.sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() }), is(56L));
    assertThat(sizeOf.sizeOf(new int[] { }), is(24L));
    assertThat(sizeOf.sizeOf(new int[] { 987654, 876543, 765432, 654321 }), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(null, null)), is(32L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), null)), is(48L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), new Object())), is(64L));
  }

  private void verify64bitJRockit4GBCompressedRefsSizes(SizeOf sizeOf) {
    verifyFlyweights(sizeOf);
    assertThat(sizeOf.sizeOf(new Object()), is(16L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(24L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(deepSizeOf(sizeOf, new Integer(1))));
    assertThat(sizeOf.sizeOf(1000), is(24L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(false)), is(24L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(true)), is(40L));
    assertThat(sizeOf.sizeOf(new Object[] { }), is(24L));
    assertThat(sizeOf.sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() }), is(40L));
    assertThat(sizeOf.sizeOf(new int[] { }), is(24L));
    assertThat(sizeOf.sizeOf(new int[] { 987654, 876543, 765432, 654321 }), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(null, null)), is(24L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), null)), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), new Object())), is(56L));
  }

  private void verify64bitIBMSizes(SizeOf sizeOf) {
    verifyFlyweights(sizeOf);
    assertThat(sizeOf.sizeOf(new Object()), is(24L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(32L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(deepSizeOf(sizeOf, new Integer(1))));
    assertThat(sizeOf.sizeOf(1000), is(32L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(false)), is(32L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(true)), is(56L));
    assertThat(sizeOf.sizeOf(new Object[] { }), is(24L));
    assertThat(sizeOf.sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() }), is(56L));
    assertThat(sizeOf.sizeOf(new int[] { }), is(24L));
    assertThat(sizeOf.sizeOf(new int[] { 987654, 876543, 765432, 654321 }), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(null, null)), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), null)), is(64L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), new Object())), is(88L));
  }

  private void verify32bitIBMSizes(SizeOf sizeOf) {
    verifyFlyweights(sizeOf);
    assertThat(sizeOf.sizeOf(new Object()), is(16L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(16L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(deepSizeOf(sizeOf, new Integer(1))));
    assertThat(sizeOf.sizeOf(1000), is(16L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(false)), is(16L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(true)), is(32L));
    assertThat(sizeOf.sizeOf(new Object[] { }), is(16L));
    assertThat(sizeOf.sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() }), is(32L));
    assertThat(sizeOf.sizeOf(new int[] { }), is(16L));
    assertThat(sizeOf.sizeOf(new int[] { 987654, 876543, 765432, 654321 }), is(32L));
    assertThat(deepSizeOf(sizeOf, new Pair(null, null)), is(24L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), null)), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), new Object())), is(56L));
  }

  private void verify64bitCompressedOopsSizes(SizeOf sizeOf) {
    verifyFlyweights(sizeOf);
    assertThat(sizeOf.sizeOf(new Object()), is(16L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(16L));
    assertThat(sizeOf.sizeOf(new Integer(1)), is(deepSizeOf(sizeOf, new Integer(1))));
    assertThat(sizeOf.sizeOf(1000), is(16L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(false)), is(16L));
    assertThat(deepSizeOf(sizeOf, new SomeClass(true)), is(32L));
    assertThat(sizeOf.sizeOf(new Object[] { }), is(16L));
    assertThat(sizeOf.sizeOf(new Object[] { new Object(), new Object(), new Object(), new Object() }), is(32L));
    assertThat(sizeOf.sizeOf(new int[] { }), is(16L));
    assertThat(sizeOf.sizeOf(new int[] { 987654, 876543, 765432, 654321 }), is(32L));
    assertThat(deepSizeOf(sizeOf, new Pair(null, null)), is(24L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), null)), is(40L));
    assertThat(deepSizeOf(sizeOf, new Pair(new Object(), new Object())), is(56L));
  }

  private void verifyFlyweights(SizeOf sizeOf) {
    assertThat(deepSizeOf(sizeOf, TimeUnit.SECONDS), is(0L));
    assertThat(deepSizeOf(sizeOf, Object.class), is(0L));
    assertThat(deepSizeOf(sizeOf, 1), is(0L));
    assertThat(deepSizeOf(sizeOf, BigInteger.ZERO), is(0L));
    assertThat(deepSizeOf(sizeOf, BigDecimal.ZERO), is(0L));
    assertThat(deepSizeOf(sizeOf, MathContext.UNLIMITED), is(0L));
    assertThat(deepSizeOf(sizeOf, Locale.ENGLISH), is(0L));
    assertThat(deepSizeOf(sizeOf, Logger.global), is(0L));
    assertThat(deepSizeOf(sizeOf, Collections.EMPTY_SET), is(0L));
    assertThat(deepSizeOf(sizeOf, Collections.EMPTY_LIST), is(0L));
    assertThat(deepSizeOf(sizeOf, Collections.EMPTY_MAP), is(0L));
    assertThat(deepSizeOf(sizeOf, String.CASE_INSENSITIVE_ORDER), is(0L));
    assertThat(deepSizeOf(sizeOf, System.err), is(0L));
    assertThat(deepSizeOf(sizeOf, Proxy.NO_PROXY), is(0L));
    assertThat(deepSizeOf(sizeOf, CodingErrorAction.REPORT), is(0L));
  }

  @Test
  public void testOnHeapConsumption() throws Exception {
    SizeOf sizeOf = new CrossCheckingSizeOf();

    int size = 80000;

    for (int j = 0; j < 5; j++) {
      container = new Object[size];
      long usedBefore = measureMemoryUse();
      for (int i = 0; i < size; i++) {
        container[i] = new EvilPair(new Object(), new SomeClass(i % 2 == 0));
      }

      long mem = 0;
      for (Object s : container) {
        mem += deepSizeOf(sizeOf, s);
      }

      long used = measureMemoryUse() - usedBefore;
      float percentage = 1 - (mem / (float) used);
      System.err.println("Run # " + (j + 1) + ": Deviation of " + String.format("%.3f", percentage * -100) +
                 "%\n" + used +
                 " bytes are actually being used, while we believe " + mem + " are");
      if (j > 1) {
        assertThat("Run # " + (j + 1) + ": Deviation of " + String.format("%.3f", percentage * -100) +
                   "% was above the +/-1.5% delta threshold \n" + used +
                   " bytes are actually being used, while we believe " + mem + " are (" +
                   (used - mem) / size + ")",
            Math.abs(percentage) < .015f, is(true));
      }
    }
  }

  private long measureMemoryUse() throws InterruptedException {
    System.gc();
    System.gc();
    System.gc();
    Thread.sleep(2000);
    System.gc();
    System.gc();
    Thread.sleep(2000);
    System.gc();
    System.gc();
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  }

  public static class SomeClass {

    public Object ref;

    public SomeClass(final boolean init) {
      if (init) {
        ref = new Object();
      }
    }
  }

  public static class Pair {
    private final Object one;
    private final Object two;

    public Pair(final Object one, final Object two) {
      this.one = one;
      this.two = two;
    }
  }

  public static final class Stupid {

    public static class internal {
      private int  someValue;
      private long otherValue;
    }

    internal internalVar   = new internal();
    int      someValue;
    long     someOther;
    long     otherValue;
    boolean  bool;
  }

  public static final class EvilPair extends Pair {

    private static final AtomicLong counter = new AtomicLong(Long.MIN_VALUE);

    private final Object oneHidden;
    private final Object twoHidden;
    private final Object copyOne;
    private final Object copyTwo;
    private final long   instanceNumber;

    private EvilPair(final Object one, final Object two) {
      super(one, two);
      instanceNumber = counter.getAndIncrement();
      if (instanceNumber % 4 == 0) {
        oneHidden = new Object();
        twoHidden = new Object();
      } else {
        oneHidden = null;
        twoHidden = null;
      }
      this.copyOne = one;
      this.copyTwo = two;
    }
  }
}
