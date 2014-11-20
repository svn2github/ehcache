package net.sf.ehcache.pool;

import net.sf.ehcache.pool.impl.ConstantSizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class SizeOfEngineLoaderTest {

    private static SizeOfEngine constantSizeOfEngine = new ConstantSizeOfEngine();

    @Test
    public void testFallsBackToDefaultSizeOfEngine() {
        final SizeOfEngine sizeOfEngine = SizeOfEngineLoader.INSTANCE.createSizeOfEngine(10, true, true);
        assertThat(sizeOfEngine, notNullValue());
        assertThat(sizeOfEngine, instanceOf(DefaultSizeOfEngine.class));
    }

    @Test
    public void testUsesServiceLoaderWhenItCan() {
        ClassLoader cl = new CheatingClassLoader();
        SizeOfEngineLoader loader = new SizeOfEngineLoader(cl);
        assertThat(loader.createSizeOfEngine(10, true, true), sameInstance(constantSizeOfEngine));
    }

    @Test
    public void testLoadsSpecificType() {
        SizeOfEngineLoader loader = new SizeOfEngineLoader(new CheatingClassLoader());
        assertThat(loader.load(MyRealFactory.class, false), is(true));
        assertThat(loader.createSizeOfEngine(10, true, true), sameInstance(constantSizeOfEngine));
    }

    @Test
    public void testLoadsMatchingSuperType() {
        SizeOfEngineLoader loader = new SizeOfEngineLoader(new CheatingClassLoader());
        assertThat(loader.load(MyFactory.class, false), is(true));
        assertThat(loader.createSizeOfEngine(10, true, true), sameInstance(constantSizeOfEngine));
    }

    @Test
    public void testFalseBackToDefaultWhenNotMatchLoaded() {
        SizeOfEngineLoader loader = new SizeOfEngineLoader(new CheatingClassLoader());
        assertThat(loader.load(NoFactory.class, true), is(false));
        assertThat(loader.createSizeOfEngine(10, true, true), instanceOf(DefaultSizeOfEngine.class));
    }

    public static interface MyFactory extends SizeOfEngineFactory {

    }

    public static final class MyRealFactory implements MyFactory {

        @Override
        public SizeOfEngine createSizeOfEngine(final int maxObjectCount, final boolean abort, final boolean silent) {
            return constantSizeOfEngine;
        }
    }

    public static interface NoFactory extends SizeOfEngineFactory {

    }

    private static class CheatingClassLoader extends SecureClassLoader {

        public CheatingClassLoader() {
            super(SizeOfEngineLoader.class.getClassLoader());
        }

        @Override
        public Enumeration<URL> getResources(final String name) throws IOException {
            final String className = SizeOfEngineFactory.class.getName();
            if (name.equals("META-INF/services/" + className)) {
                return super.getResources("services/" + className + ".txt");
            }
            return super.getResources(name);
        }
    }
}
