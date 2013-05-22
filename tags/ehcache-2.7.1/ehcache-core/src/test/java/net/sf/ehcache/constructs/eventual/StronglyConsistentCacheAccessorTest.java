package net.sf.ehcache.constructs.eventual;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.ElementValueComparatorConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * StronglyConsistentCacheAccessorTest
 */
public class StronglyConsistentCacheAccessorTest {

    @Test
    public void refusesStandaloneCache() {
        try {
            new StronglyConsistentCacheAccessor(new Cache("standalone", 100, false, false, 300, 600));
            fail("Underlying cache not clustered and eventual");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void refusesClusteredButNoEventualCache() {
        TerracottaConfiguration terracottaConfiguration = mock(TerracottaConfiguration.class);
        CacheConfiguration cacheConfiguration = mock(CacheConfiguration.class);
        Ehcache underlyingCache = mock(Ehcache.class);

        when(underlyingCache.getName()).thenReturn("testCache");
        when(underlyingCache.getCacheConfiguration()).thenReturn(cacheConfiguration);
        when(cacheConfiguration.getTerracottaConfiguration()).thenReturn(terracottaConfiguration);
        when(terracottaConfiguration.getConsistency()).thenReturn(TerracottaConfiguration.Consistency.STRONG);

        try {
            new StronglyConsistentCacheAccessor(underlyingCache);
            fail("Underlying cache clustered but not eventual");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void acceptClusteredAndEventualUnderlyingCache() {
        Ehcache underlyingCache = buildMockCache();

        new StronglyConsistentCacheAccessor(underlyingCache);
    }

    @Test
    public void putMethodsDoWriteLock() throws Exception {
        Ehcache ehcache = buildMockCache();
        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(ehcache);

        String key = "test";
        Element element = new Element(key, "test", 0);

        String put = "put";
        Set<Method> putMethods = getMethodsMatching(put);
        for (Method putMethod : putMethods) {
            int paramLength = putMethod.getParameterTypes().length;
            Object[] params = new Object[paramLength];
            if (Collection.class.isAssignableFrom(putMethod.getParameterTypes()[0])) {
                params[0] = Collections.singleton(element);
            } else {
                params[0] = element;
            }
            if (paramLength > 1 && putMethod.getParameterTypes()[1].equals(Boolean.TYPE)) {
                params[1] = false;
            }
            System.out.println("Invoking " + putMethod.getName());
            putMethod.invoke(cacheAccessor, params);
            verify(ehcache).acquireWriteLockOnKey(key);
            verify(ehcache).releaseWriteLockOnKey(key);
            reset(ehcache);
        }
    }

    @Test
    public void removeMethodsDoWriteLock() throws Exception {
        Ehcache ehcache = buildMockCache();
        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(ehcache);

        String key = "test";
        Element element = new Element(key, "someValue", 0);

        String remove = "remove";
        Set<Method> removeMethods = getMethodsMatching(remove, "Property");
        for (Method removeMethod : removeMethods) {
            System.out.println("Checking " + removeMethod.getName() + "(" + Arrays.toString(removeMethod.getParameterTypes()) + ")");
            int paramLength = removeMethod.getParameterTypes().length;
            if (paramLength == 0 || (paramLength == 1 && removeMethod.getParameterTypes()[0].isPrimitive())) {
                continue;
            }
            Object[] params = new Object[paramLength];
            if (Collection.class.isAssignableFrom(removeMethod.getParameterTypes()[0])) {
                params[0] = Collections.singleton(key);
            } else if (Element.class.isAssignableFrom(removeMethod.getParameterTypes()[0])) {
                params[0] = element;
            } else {
                params[0] = key;
            }
            if (paramLength > 1 && removeMethod.getParameterTypes()[1].equals(Boolean.TYPE)) {
                params[1] = false;
            }
            System.out.println("Invoking " + removeMethod.getName() + " with " + Arrays.toString(params));
            removeMethod.invoke(cacheAccessor, params);
            verify(ehcache).acquireWriteLockOnKey(key);
            verify(ehcache).releaseWriteLockOnKey(key);
            reset(ehcache);
        }
    }

    @Test
    public void getMethodsDoReadLock() throws Exception {
        Ehcache ehcache = buildMockCache();
        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(ehcache);

        String key = "key";
        String get = "get";
        Set<Method> getMethods = getMethodsMatching(get, "Internal", "S", "Keys", "Cache", "Manager", "Name", "Guid");
        for (Method getMethod : getMethods) {
            System.out.println("Checking " + getMethod.getName() + "(" + Arrays.toString(getMethod.getParameterTypes()) + ")");
            int paramLength = getMethod.getParameterTypes().length;
            Object[] params = new Object[paramLength];
            if (Collection.class.isAssignableFrom(getMethod.getParameterTypes()[0])) {
                params[0] = Collections.singleton(key);
            } else {
                params[0] = key;
            }

            System.out.println("Invoking " + getMethod.getName() + " with " + Arrays.toString(params));
            getMethod.invoke(cacheAccessor, params);
            verify(ehcache).acquireReadLockOnKey(key);
            verify(ehcache).releaseReadLockOnKey(key);
            reset(ehcache);
        }
    }

    private Set<Method> getMethodsMatching(String put, String... excludes) {
        Set<Method> results = new HashSet<Method>();
        Method[] methods = Ehcache.class.getMethods();
        for (Method method : methods) {
            if (method.getName().contains(put)) {
                boolean exclude = false;
                for (String exclusion : excludes) {
                    if (method.getName().contains(exclusion)) {
                        exclude = true;
                    }
                }
                if (!exclude) {
                    results.add(method);
                }
            }
        }
        return results;
    }

    private Ehcache buildMockCache() {TerracottaConfiguration terracottaConfiguration = mock(TerracottaConfiguration.class);
        CacheConfiguration cacheConfiguration = mock(CacheConfiguration.class);
        Ehcache underlyingCache = mock(Ehcache.class);

        when(underlyingCache.getName()).thenReturn("testCache");
        when(underlyingCache.getCacheConfiguration()).thenReturn(cacheConfiguration);
        when(cacheConfiguration.getTerracottaConfiguration()).thenReturn(terracottaConfiguration);
        when(cacheConfiguration.getElementValueComparatorConfiguration()).thenReturn(new ElementValueComparatorConfiguration());
        when(terracottaConfiguration.getConsistency()).thenReturn(TerracottaConfiguration.Consistency.EVENTUAL);
        return underlyingCache;
    }
}
