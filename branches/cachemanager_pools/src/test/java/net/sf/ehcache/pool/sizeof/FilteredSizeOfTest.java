package net.sf.ehcache.pool.sizeof;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import net.sf.ehcache.pool.sizeof.JvmInformation;
import net.sf.ehcache.pool.sizeof.SizeOf;
import net.sf.ehcache.pool.sizeof.SizeOfAgent;
import net.sf.ehcache.pool.sizeof.filter.AnnotationSizeOfFilter;
import net.sf.ehcache.pool.sizeof.filter.IgnoreSizeOf;
import net.sf.ehcache.pool.sizeof.filter.ResourceSizeOfFilter;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class FilteredSizeOfTest extends AbstractSizeOfTest {

  @BeforeClass
  public static void setup() {
    Assume.assumeThat(JvmInformation.MINIMUM_OBJECT_SIZE, is(JvmInformation.OBJECT_ALIGNMENT));
    
    new CrossCheckingSizeOf().deepSizeOf(new Object());
    System.err.println("Testing for a " + System.getProperty("java.version") + " JDK (agent: " + SizeOfAgent.isAvailable() + ") on a "
                       + System.getProperty("sun.arch.data.model") + "bit VM (compressed-oops: " + COMPRESSED_OOPS + ")");
  }
  
  @Test
  public void testAnnotationFiltering() throws Exception {
    SizeOf sizeOf = new CrossCheckingSizeOf(new AnnotationSizeOfFilter());
    if (System.getProperty("java.version").startsWith("1.5")) {
      if (System.getProperty("sun.arch.data.model").equals("64")) {
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(192L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(200L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredRefferer()), is(224L));
      } else {
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredRefferer()), is(184L));
      }
    } else {
      if (System.getProperty("sun.arch.data.model").equals("64")) {
        if (COMPRESSED_OOPS) {
          assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(168L));
          assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(176L));
          assertThat(sizeOf.deepSizeOf(new AnnotationFilteredRefferer()), is(192L));
        } else {
          assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(192L));
          assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(200L));
          assertThat(sizeOf.deepSizeOf(new AnnotationFilteredRefferer()), is(224L));
        }
      } else {
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredClass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredSubclass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new AnnotationFilteredRefferer()), is(184L));
      }
    }
  }
  
  @Test
  public void testResourceFiltering() throws Exception {
    SizeOf sizeOf = new CrossCheckingSizeOf(new ResourceSizeOfFilter(FilteredSizeOfTest.class.getClassLoader().getResource("sizeof.filter.fields")));
    if (System.getProperty("java.version").startsWith("1.5")) {
      if (System.getProperty("sun.arch.data.model").equals("64")) {
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(192L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(200L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredRefferer()), is(224L));
      } else {
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredRefferer()), is(184L));
      }
    } else {
      if (System.getProperty("sun.arch.data.model").equals("64")) {
        if (COMPRESSED_OOPS) {
          assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(168L));
          assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(176L));
          assertThat(sizeOf.deepSizeOf(new ResourceFilteredRefferer()), is(192L));
        } else {
          assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(192L));
          assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(200L));
          assertThat(sizeOf.deepSizeOf(new ResourceFilteredRefferer()), is(224L));
        }
      } else {
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredClass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredSubclass()), is(168L));
        assertThat(sizeOf.deepSizeOf(new ResourceFilteredRefferer()), is(184L));
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
  public static class AnnotationFilteredRefferer {
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
  public static class ResourceFilteredRefferer {
    private final ResourceFilteredSubclass reference = new ResourceFilteredSubclass();
  }
}
