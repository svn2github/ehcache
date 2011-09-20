package net.sf.ehcache.osgitests;

import static org.ops4j.pax.exam.CoreOptions.allFrameworksVersions;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.spi.container.PaxExamRuntime.createTestSystem;
import static org.ops4j.pax.exam.spi.container.PaxExamRuntime.getTestContainerFactory;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.util.ProductInfo;

import org.junit.Test;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.TestProbeProvider;
import org.ops4j.pax.exam.options.BootDelegationOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BasicOsgiTest {

    @Test
    public void testBasic() throws Exception {
        if (System.getProperty("java.vm.version").startsWith("1.5.")) {
            // The version of pax-exam we're using is 1.6 dependent
            System.err.println("");
            System.err.println("***************************************");
            System.err.println("NOT RUNNING test since this is jdk 1.5");
            System.err.println("***************************************");
            System.err.println("");
            return;
        }

        ExamSystem system = createTestSystem(new Option[] {allFrameworksVersions(),
                mavenBundle("net.sf.ehcache", "ehcache-core", new ProductInfo().getVersion()),

                // XXX: remove if our JTA dependency can be made completely optional
                // At the moment the JVM verifier will still try to load some JTA classes (that are normally visible from rt.jar) even for
                // code that never executes (barring no defined TX caches)
                new BootDelegationOption("javax.transaction.xa")});

        TestProbeProvider p = makeProbe(system);

        for (TestContainer testContainer : getTestContainerFactory().create(system)) {
            try {
                testContainer.start();
                testContainer.install(p.getStream());
                for (TestAddress test : p.getTests()) {
                    testContainer.call(test);
                }
            } finally {
                testContainer.stop();
            }
        }
    }

    private TestProbeProvider makeProbe(ExamSystem system) throws IOException {
        TestProbeBuilder probe = system.createProbe(new Properties());
        probe.addTest(Probe.class, "test");
        probe.setHeader("Import-Package", "net.sf.ehcache,net.sf.ehcache.config");
        return probe.build();
    }

    public static class Probe {

        public void test() {
            ClassLoader prev = Thread.currentThread().getContextClassLoader();
            try {
                // Avoid ever loading classes from TCCL (which happens to be system classloader here)
                Thread.currentThread().setContextClassLoader(new NullClassLoader());
                test0();
            } finally {
                Thread.currentThread().setContextClassLoader(prev);
            }
        }

        private void test0() {
            CacheManager cm = CacheManager.getInstance();

            CacheConfiguration config = new CacheConfiguration();
            config.eternal(true);
            config.overflowToDisk(false);
            config.overflowToOffHeap(false);
            config.diskPersistent(false);
            config.setMaxEntriesLocalHeap(1);
            config.name("cache");

            cm.addCache(new Cache(config));

            Cache cache = cm.getCache("cache");
            cache.put(new Element("key", "value"));
            assertSize(1, cache);
            cache.remove("key");
            assertSize(0, cache);

            cm.shutdown();
        }

        private void assertSize(int expect, Cache cache) {
            int size = cache.getSize();
            if (size != expect) {
                throw new AssertionError(size);
            }
        }

    }

    private static class NullClassLoader extends ClassLoader {
        public NullClassLoader() {
            super(null);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }
    }

}
