package net.sf.ehcache.pool.sizeof;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLClassLoader;

import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import net.sf.ehcache.pool.sizeof.annotationfiltered.AnnotationFilteredPackage;
import net.sf.ehcache.pool.sizeof.filter.AnnotationSizeOfFilter;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;
import net.sf.ehcache.pool.sizeof.filter.ResourceSizeOfFilter;
import net.sf.ehcache.pool.sizeof.resourcefiltered.ResourceFilteredPackage;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

public class FilteredSizeOfTest extends AbstractSizeOfTest {

    private static long deepSizeOf(SizeOf sizeOf, Object... obj) {
        return sizeOf.deepSizeOf(1000, true, obj).getCalculated();
    }

    @BeforeClass
    public static void setup() {
        deepSizeOf(new CrossCheckingSizeOf(), new Object());
        System.out.println("Testing for a " + System.getProperty("java.version") + " JDK "
                + ") on a " + System.getProperty("sun.arch.data.model") + "-bit VM "
                + "(compressed-oops: " + COMPRESSED_OOPS
                + ", Hotspot CMS: " + HOTSPOT_CMS
                + ")");
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
        assertThat(deepSizeOf(sizeOf, new Parent()), equalTo(0L));
        assertThat(deepSizeOf(sizeOf, new Child()), equalTo(0L));
        assertThat(deepSizeOf(sizeOf, new ChildChild()), equalTo(0L));
        assertThat(deepSizeOf(sizeOf, new ChildChildChild()), equalTo(0L));
    }

    @Test
    public void testCustomAnnotationFiltering() throws Exception {
        SizeOf sizeOf = new CrossCheckingSizeOf(new AnnotationSizeOfFilter());
        assertThat(deepSizeOf(sizeOf, new MatchingPatternOrNotAnnotationFilteredField()), allOf(greaterThan(128L), lessThan(16 * 1024L)));
        assertThat(deepSizeOf(sizeOf, new MatchingPatternAnnotation()), equalTo(0L));
        assertThat(deepSizeOf(sizeOf, new MatchingPatternAnnotationChild()), equalTo(0L));
        assertThat(deepSizeOf(sizeOf, new MatchingPatternAnnotationNoInheritedChild()), allOf(greaterThan(8L)));
        assertThat(deepSizeOf(sizeOf, new NonMatchingPatternAnnotation1()), allOf(greaterThan(8L)));
        assertThat(deepSizeOf(sizeOf, new NonMatchingPatternAnnotation2()), allOf(greaterThan(8L)));
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

    private static SizeOfEngine getLoaderIsolatedEngine(int depth, boolean abort) {
        ClassLoader current = FilteredSizeOfTest.class.getClassLoader();
        Assume.assumeThat(current, instanceOf(URLClassLoader.class));
        ClassLoader mirror = new URLClassLoader(((URLClassLoader) current).getURLs(), current) {

            @Override
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if ("net.sf.ehcache.pool.impl.DefaultSizeOfEngine".equals(name)) {
                    Class klazz = findLoadedClass(name);
                    if (klazz == null) {
                        klazz = findClass(name);
                        if (resolve) {
                            resolveClass(klazz);
                        }
                    }
                    return klazz;
                } else {
                    return super.loadClass(name, resolve);
                }
            }
        };

        try {
            return (SizeOfEngine) mirror.loadClass("net.sf.ehcache.pool.impl.DefaultSizeOfEngine").getConstructor(Integer.TYPE, Boolean.TYPE).newInstance(1000, false);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
  
    private static void testSizeOfEngineFiltering(String filter) {
        System.setProperty(DefaultSizeOfEngine.USER_FILTER_RESOURCE, filter);
        try {
            SizeOfEngine engine = getLoaderIsolatedEngine(1000, false);

            assertThat(engine.sizeOf(new ResourceFilteredField(), null, null).getCalculated(), allOf(greaterThan(128L), lessThan(16 * 1024L)));
            assertThat(engine.sizeOf(new ResourceFilteredClass(), null, null).getCalculated(), equalTo(0L));
            assertThat(engine.sizeOf(new ResourceFilteredPackage(), null, null).getCalculated(), equalTo(0L));

            assertThat(engine.sizeOf(new ResourceFilteredFieldSubclass(), null, null).getCalculated(), allOf(greaterThan(128L), lessThan(16 * 1024L)));
    
            long emptyReferrerSize = engine.sizeOf(new Referrer(null), null, null).getCalculated();
            assertThat(engine.sizeOf(new Referrer(new ResourceFilteredClass()), null, null).getCalculated(), equalTo(emptyReferrerSize));
            assertThat(engine.sizeOf(new Referrer(new ResourceFilteredPackage()), null, null).getCalculated(), equalTo(emptyReferrerSize));
        } finally {
            System.clearProperty(DefaultSizeOfEngine.USER_FILTER_RESOURCE);
        }
    }
    
    @Test
    public void testResourceFilterLoading() throws Exception {
        testSizeOfEngineFiltering("sizeof.filter.fields");
    }
  
    @Test
    public void testFileFilterLoading() throws Exception {
        File tempFilterFile = File.createTempFile("FilteredSizeOfTest", ".filter");
        try {
            tempFilterFile.deleteOnExit();
            FileOutputStream fout = new FileOutputStream(tempFilterFile);
            try {
                InputStream in = FilteredSizeOfTest.class.getResourceAsStream("/sizeof.filter.fields");
                try {
                    while (true) {
                        int read = in.read();
                        if (read < 0) {
                            break;
                        } else {
                            fout.write(read);
                        }
                    }
                } finally {
                    in.close();
                }
            } finally {
                fout.close();
            }
            testSizeOfEngineFiltering(tempFilterFile.getAbsolutePath());
        } finally {
            assertThat(tempFilterFile.delete(), equalTo(true));
        }
    }

    @Test
    public void testUrlFilterLoading() throws Exception {
        testSizeOfEngineFiltering(FilteredSizeOfTest.class.getResource("/sizeof.filter.fields").toExternalForm());
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

    @IgnoreSizeOf(inherited = true)
    public static class Parent {
    }

    public static class Child extends Parent {
    }

    @IgnoreSizeOf
    public static class ChildChild extends Child {
    }

    public static class ChildChildChild extends ChildChild {
    }

    @com.terracotta.ehcache.special.annotation.IgnoreSizeOf(inherited=true)
    public static class MatchingPatternAnnotation {
    }

    public static class MatchingPatternAnnotationChild extends MatchingPatternAnnotation{
    }

    @com.terracotta.ehcache.special.annotation.no.inherited.IgnoreSizeOf
    public static class MatchingPatternAnnotationNoInherited {
    }

    public static class MatchingPatternAnnotationNoInheritedChild extends MatchingPatternAnnotationNoInherited{
    }

    @com.terracotta.ehcache.special.annotation.IgnoreSizeOffff
    public static class NonMatchingPatternAnnotation1 {
    }

    @com.terracotta.special.annotation.IgnoreSizeOf
    public static class NonMatchingPatternAnnotation2 {
    }

    public static class MatchingPatternOrNotAnnotationFilteredField {
        @com.terracotta.ehcache.special.annotation.IgnoreSizeOf
        private final byte[] matchingBigArray = new byte[16 * 1024];
        @com.terracotta.special.annotation.IgnoreSizeOf
        private final byte[] nonMatchingSmallArray = new byte[128];
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
