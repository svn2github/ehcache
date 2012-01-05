package net.sf.ehcache.pool.sizeof;

import net.sf.ehcache.pool.sizeof.annotationfiltered.AnnotationFilteredPackage;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

import net.sf.ehcache.pool.sizeof.filter.AnnotationSizeOfFilter;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;
import net.sf.ehcache.pool.sizeof.filter.ResourceSizeOfFilter;

import net.sf.ehcache.pool.sizeof.resourcefiltered.ResourceFilteredPackage;
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
    
    assertThat(deepSizeOf(sizeOf, new AnnotationFilteredField()), allOf(greaterThan(128L), lessThan(16 * 1024L)));
    assertThat(deepSizeOf(sizeOf, new AnnotationFilteredClass()), equalTo(0L));
    assertThat(deepSizeOf(sizeOf, new AnnotationFilteredPackage()), equalTo(0L));
    
    assertThat(deepSizeOf(sizeOf, new AnnotationFilteredFieldSubclass()), allOf(greaterThan(128L), lessThan(16 * 1024L)));
    
    long emptyReferrerSize = deepSizeOf(sizeOf, new Referrer(null));
    assertThat(deepSizeOf(sizeOf, new Referrer(new AnnotationFilteredClass())), equalTo(emptyReferrerSize));
    assertThat(deepSizeOf(sizeOf, new Referrer(new AnnotationFilteredPackage())), equalTo(emptyReferrerSize));
  }

  @Test
  public void testResourceFiltering() throws Exception {
    SizeOf sizeOf = new CrossCheckingSizeOf(new ResourceSizeOfFilter(FilteredSizeOfTest.class.getClassLoader().getResource("sizeof.filter.fields")));

    assertThat(deepSizeOf(sizeOf, new ResourceFilteredField()), allOf(greaterThan(128L), lessThan(16 * 1024L)));
    assertThat(deepSizeOf(sizeOf, new ResourceFilteredClass()), equalTo(0L));
    assertThat(deepSizeOf(sizeOf, new ResourceFilteredPackage()), equalTo(0L));
    
    assertThat(deepSizeOf(sizeOf, new ResourceFilteredFieldSubclass()), allOf(greaterThan(128L), lessThan(16 * 1024L)));
    
    long emptyReferrerSize = deepSizeOf(sizeOf, new Referrer(null));
    assertThat(deepSizeOf(sizeOf, new Referrer(new ResourceFilteredClass())), equalTo(emptyReferrerSize));
    assertThat(deepSizeOf(sizeOf, new Referrer(new ResourceFilteredPackage())), equalTo(emptyReferrerSize));
  }

  public static class AnnotationFilteredField {
    @IgnoreSizeOf
    private final byte[] bigArray = new byte[16 * 1024];

    private final byte[] smallArray = new byte[128];
  }

  public static class AnnotationFilteredFieldSubclass extends AnnotationFilteredField {
  }
  
  @IgnoreSizeOf
  public static class AnnotationFilteredClass {
      private final byte[] bigArray = new byte[16 * 1024];
  }
  
  public static class ResourceFilteredField {
    private final byte[] bigArray = new byte[16 * 1024];

    private final byte[] smallArray = new byte[128];
  }

  public static class ResourceFilteredFieldSubclass extends ResourceFilteredField {
  }
  
  public static class ResourceFilteredClass {
      private final byte[] bigArray = new byte[6 * 1024];
  }
  
  public static class Referrer {
    
    private final Object reference;
    
      public Referrer(Object obj) {
          this.reference = obj;
      }
  }

}
