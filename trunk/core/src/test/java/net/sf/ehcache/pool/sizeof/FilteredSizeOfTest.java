package net.sf.ehcache.pool.sizeof;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import net.sf.ehcache.pool.sizeof.filter.AnnotationSizeOfFilter;
import net.sf.ehcache.pool.sizeof.filter.IgnoreSizeOf;
import net.sf.ehcache.pool.sizeof.filter.ResourceSizeOfFilter;

import org.junit.BeforeClass;
import org.junit.Test;

public class FilteredSizeOfTest extends AbstractSizeOfTest {

  @BeforeClass
  public static void setup() {
    new CrossCheckingSizeOf().deepSizeOf(new Object());
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
      if (System.getProperty("sun.arch.data.model").equals("64")) {
        System.out.println("asserting 1.5 / 64-bit values");
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(192L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(200L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredReferrer()), is(224L));
      } else {
        System.out.println("asserting 1.5 / 32-bit values");
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredReferrer()), is(184L));
      }
    } else {
      if (System.getProperty("sun.arch.data.model").equals("64")) {
        if (COMPRESSED_OOPS) {
          if (HOTSPOT_CMS) {
            System.out.println("asserting 1.6+ / 64-bit / compressed OOPs / Hotspot CMS values");
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(168L));
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(176L));
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredReferrer()), is(200L));
          } else {
            System.out.println("asserting 1.6+ / 64-bit / compressed OOPs / non-Hotspot CMS values");
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(168L));
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(176L));
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredReferrer()), is(192L));
          }
        } else {
          if (HOTSPOT_CMS) {
            System.out.println("asserting 1.6+ / 64-bit / plain OOPs / Hotspot CMS values");
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(192L));
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(200L));
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredReferrer()), is(224L));
          } else {
            System.out.println("asserting 1.6+ / 64-bit / plain OOPs / non-Hotspot CMS values");
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(192L));
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(200L));
            assertThat(sizeOf.deepSizeOf(new AnnotationFilteredReferrer()), is(224L));
          }
        }
      } else {
        System.out.println("asserting 1.6+ / 32-bit values");
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredReferrer()), is(184L));
      }
    }
  }

  @Test
  public void testResourceFiltering() throws Exception {
    SizeOf sizeOf = new CrossCheckingSizeOf(new ResourceSizeOfFilter(FilteredSizeOfTest.class.getClassLoader().getResource("sizeof.filter.fields")));
    if (System.getProperty("java.version").startsWith("1.5")) {
      if (System.getProperty("sun.arch.data.model").equals("64")) {
        System.out.println("asserting 1.5 / 64-bit values");
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(192L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(200L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredReferrer()), is(224L));
      } else {
        System.out.println("asserting 1.5 / 32-bit values");
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredReferrer()), is(184L));
      }
    } else {
      if (System.getProperty("sun.arch.data.model").equals("64")) {
        if (COMPRESSED_OOPS) {
          if (HOTSPOT_CMS) {
            System.out.println("asserting 1.6+ / 64-bit / compressed OOPs / Hotspot CMS values");
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(168L));
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(176L));
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredReferrer()), is(200L));
          } else {
            System.out.println("asserting 1.6+ / 64-bit / compressed OOPs / non-Hotspot CMS values");
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(168L));
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(176L));
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredReferrer()), is(200L));
          }
        } else {
          if (HOTSPOT_CMS) {
            System.out.println("asserting 1.6+ / 64-bit / plain OOPs / Hotspot CMS values");
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(192L));
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(200L));
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredReferrer()), is(224L));
          } else {
            System.out.println("asserting 1.6+ / 64-bit / plain OOPs / non-Hotspot CMS values");
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(192L));
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(200L));
            assertThat(sizeOf.deepSizeOf(new ResourceFilteredReferrer()), is(224L));
          }
        }
      } else {
        System.out.println("asserting 1.6+ / 32-bit values");
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredReferrer()), is(184L));
      }
    }
  }

  @SuppressWarnings("unused")
  public static class AnnotationFilteredClass {
    @IgnoreSizeOf
    private final byte[] bigArray = new byte[16 * 1024];

    @IgnoreSizeOf
    private final byte[] mediumArray = new byte[1024];

    private final byte[] smallArray = new byte[128];
  }

  @SuppressWarnings("unused")
  public static class AnnotationFilteredSubclass extends AnnotationFilteredClass {
    private final int field = 0;
  }

  @SuppressWarnings("unused")
  public static class AnnotationFilteredReferrer {
    private final AnnotationFilteredSubclass reference = new AnnotationFilteredSubclass();
  }

  @SuppressWarnings("unused")
  public static class ResourceFilteredClass {
    private final byte[] bigArray = new byte[16 * 1024];

    private final byte[] mediumArray = new byte[1024];

    private final byte[] smallArray = new byte[128];
  }

  @SuppressWarnings("unused")
  public static class ResourceFilteredSubclass extends ResourceFilteredClass {
    private final int field = 0;
  }

  @SuppressWarnings("unused")
  public static class ResourceFilteredReferrer {
    private final ResourceFilteredSubclass reference = new ResourceFilteredSubclass();
  }
}
