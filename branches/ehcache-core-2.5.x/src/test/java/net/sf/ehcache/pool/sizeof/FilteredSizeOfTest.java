package net.sf.ehcache.pool.sizeof;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import net.sf.ehcache.pool.sizeof.filter.AnnotationSizeOfFilter;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;
import net.sf.ehcache.pool.sizeof.filter.ResourceSizeOfFilter;

import org.junit.BeforeClass;
import org.junit.Test;

public class FilteredSizeOfTest extends AbstractSizeOfTest {

  private static long deepSizeOf(SizeOf sizeOf, Object... obj) {
    return sizeOf.deepSizeOf(1000, true, obj).getCalculated();
  }

  @BeforeClass
  public static void setup() {
    deepSizeOf(new CrossCheckingSizeOf(), new Object());
    System.out.println("Testing for a " + System.getProperty("java.version") + " JDK " +
                       ") on a " + System.getProperty("sun.arch.data.model") + "-bit VM " +
                       "(compressed-oops: " + COMPRESSED_OOPS +
                       ", Hotspot CMS: " + HOTSPOT_CMS +
                       ")");
  }

  @Test
  public void testAnnotationFiltering() throws Exception {
    SizeOf sizeOf = new CrossCheckingSizeOf(new AnnotationSizeOfFilter());
    if (System.getProperty("java.version").startsWith("1.5")) {
      if (IS_64_BIT) {
        System.out.println("asserting 1.5 / 64-bit values");
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(192L));
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(200L));
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(224L));
      } else {
        System.out.println("asserting 1.5 / 32-bit values");
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(168L));
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(168L));
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(184L));
      }
    } else {
      if (IS_64_BIT) {
        if (COMPRESSED_OOPS) {
          if (HOTSPOT_CMS) {
            System.out.println("asserting 1.6+ / 64-bit / compressed OOPs / Hotspot CMS values");
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(168L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(176L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(200L));
          } else {
            System.out.println("asserting 1.6+ / 64-bit / compressed OOPs / non-Hotspot CMS values");
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(168L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(176L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(192L));
          }
        } else if (IS_JROCKIT) {
            System.out.println("asserting JRockit 1.6+ / 64-bit / 4GB compressed refs values");
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(184L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(184L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(208L));
        } else if (IS_IBM) {
            System.out.println("asserting IBM 1.6+ / 64-bit / (con-compressed) values");
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(200L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(208L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(240L));
        } else {
          if (HOTSPOT_CMS) {
            System.out.println("asserting 1.6+ / 64-bit / plain OOPs / Hotspot CMS values");
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(192L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(200L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(224L));
          } else {
            System.out.println("asserting 1.6+ / 64-bit / plain OOPs / non-Hotspot CMS values");
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(192L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(200L));
            assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(224L));
          }
        }
      } else if (IS_IBM) {
          System.out.println("asserting IBM 1.6+ / 32-bit values");
          assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(168L));
          assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(176L));
          assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(192L));
      } else if (IS_JROCKIT) {
        System.out.println("asserting JRockit 1.6+ / 32-bit values");
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(184L));
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(184L));
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(208L));
      } else {
        System.out.println("asserting 1.6+ / 32-bit values");
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), is(168L));
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredSubclass()), is(168L));
        assertThat(deepSizeOf(sizeOf, new AnnotationFilteredReferrer()), is(184L));
      }
    }
  }

  @Test
  public void testResourceFiltering() throws Exception {
    SizeOf sizeOf = new CrossCheckingSizeOf(new ResourceSizeOfFilter(FilteredSizeOfTest.class.getClassLoader().getResource("sizeof.filter.fields")));
    if (System.getProperty("java.version").startsWith("1.5")) {
      if (IS_64_BIT) {
        System.out.println("asserting 1.5 / 64-bit values");
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(192L));
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(200L));
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(224L));
      } else {
        System.out.println("asserting 1.5 / 32-bit values");
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(168L));
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(168L));
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(184L));
      }
    } else {
      if (IS_64_BIT) {
        if (COMPRESSED_OOPS) {
          if (HOTSPOT_CMS) {
            System.out.println("asserting 1.6+ / 64-bit / compressed OOPs / Hotspot CMS values");
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(168L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(176L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(200L));
          } else {
            System.out.println("asserting 1.6+ / 64-bit / compressed OOPs / non-Hotspot CMS values");
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(168L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(176L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(192L));
          }
        } else if (IS_JROCKIT) {
            System.out.println("asserting JRockit 1.6+ / 64-bit / 4GB compressed refs values");
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(184L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(184L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(208L));
        } else if (IS_IBM) {
            System.out.println("asserting IBM 1.6+ / 64-bit / (non-compressed) refs values");
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(200L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(208L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(240L));
        } else {
          if (HOTSPOT_CMS) {
            System.out.println("asserting 1.6+ / 64-bit / plain OOPs / Hotspot CMS values");
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(192L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(200L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(224L));
          } else {
            System.out.println("asserting 1.6+ / 64-bit / plain OOPs / non-Hotspot CMS values");
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(192L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(200L));
            assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(224L));
          }
        }
      } else if (IS_IBM) {
          System.out.println("asserting IBM 1.6+ / 32-bit values");
          assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(168L));
          assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(176L));
          assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(192L));
      } else if (IS_JROCKIT) {
        System.out.println("asserting JRockit 1.6+ / 32-bit values");
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(184L));
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(184L));
        assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(208L));
      } else {
          System.out.println("asserting 1.6+ / 32-bit values");
          assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), is(168L));
          assertThat(deepSizeOf(sizeOf, new ResourceFilteredSubclass()), is(168L));
          assertThat(deepSizeOf(sizeOf, new ResourceFilteredReferrer()), is(184L));
        }
    }
  }

  public static class AnnotationFilteredClass {
    @IgnoreSizeOf
    private final byte[] bigArray = new byte[16 * 1024];

    @IgnoreSizeOf
    private final byte[] mediumArray = new byte[1024];

    private final byte[] smallArray = new byte[128];
  }

  public static class AnnotationFilteredSubclass extends AnnotationFilteredClass {
    private final int field = 0;
  }

  public static class AnnotationFilteredReferrer {
    private final AnnotationFilteredSubclass reference = new AnnotationFilteredSubclass();
  }

  public static class ResourceFilteredClass {
    private final byte[] bigArray = new byte[16 * 1024];

    private final byte[] mediumArray = new byte[1024];

    private final byte[] smallArray = new byte[128];
  }

  public static class ResourceFilteredSubclass extends ResourceFilteredClass {
    private final int field = 0;
  }

  public static class ResourceFilteredReferrer {
    private final ResourceFilteredSubclass reference = new ResourceFilteredSubclass();
  }
}
