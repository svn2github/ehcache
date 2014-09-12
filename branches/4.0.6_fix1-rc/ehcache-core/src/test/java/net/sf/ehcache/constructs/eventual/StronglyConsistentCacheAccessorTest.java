package net.sf.ehcache.constructs.eventual;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
    public void testPutIfAbsent() {
        Element element = new Element("key", "value");
        Ehcache underlyingCache = buildMockCache();
        when(underlyingCache.getQuiet((Object)"key")).thenReturn(null, element);

        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(underlyingCache);
        element = cacheAccessor.putIfAbsent(element);
        assertThat(element, nullValue());


        element = cacheAccessor.putIfAbsent(new Element("key", "otherValue"));
        assertThat(element.getObjectValue(), equalTo((Object)"value"));

        try {
            cacheAccessor.putIfAbsent(new Element(null, null));
            fail("Expected NPE with null key");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testSingleArgReplace() {
        Element element = new Element("key", "value");
        Ehcache underlyingCache = buildMockCache();
        when(underlyingCache.getQuiet((Object)"key")).thenReturn(null, element);

        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(underlyingCache);
        element = cacheAccessor.replace(element);
        assertThat(element, nullValue());


        element = cacheAccessor.replace(new Element("key", "otherValue"));
        assertThat(element.getObjectValue(), equalTo((Object)"value"));

        try {
            cacheAccessor.replace(new Element(null, null));
            fail("Expected NPE with null key");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testTwoArgReplace() {
        Element element = new Element("key", "value");
        Ehcache underlyingCache = buildMockCache();
        when(underlyingCache.getQuiet((Object)"key")).thenReturn(null, element);

        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(underlyingCache);
        assertThat(cacheAccessor.replace(element, new Element("key", "otherValue")), is(false));
        assertThat(cacheAccessor.replace(element, new Element("key", "otherValue")), is(true));

        try {
            cacheAccessor.replace(new Element(null, null), new Element("key", "other"));
            fail("Expected NPE with null key");
        } catch (NullPointerException e) {
            // Expected
        }

        try {
            cacheAccessor.replace(new Element("key", "other"), new Element(null, null));
            fail("Expected NPE with null key");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testRemoveElement() {
        Element element = new Element("key", "value");
        Ehcache underlyingCache = buildMockCache();
        when(underlyingCache.getQuiet((Object)"key")).thenReturn(new Element("key", "other"), element);
        when(underlyingCache.remove((Object)"key")).thenReturn(true);

        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(underlyingCache);
        assertThat(cacheAccessor.removeElement(element), is(false));
        assertThat(cacheAccessor.removeElement(element), is(true));

        try {
            cacheAccessor.removeElement(new Element(null, null));
            fail("Expected NPE with null key");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void putMethodsDoWriteLock() throws Exception {
        Ehcache ehcache = buildMockCache();
        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(ehcache);

        String key = "test";
        Element element = new Element(key, "test");

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
    public void replaceMethodsDoWriteLock() throws Exception {
        Ehcache ehcache = buildMockCache();
        StronglyConsistentCacheAccessor cacheAccessor = new StronglyConsistentCacheAccessor(ehcache);

        String key = "test";
        Element element = new Element(key, "test");

        Set<Method> replaceMethods = getMethodsMatching("replace");
        for (Method replaceMethod : replaceMethods) {
            int paramLength = replaceMethod.getParameterTypes().length;
            Object[] params = new Object[paramLength];
            if (Collection.class.isAssignableFrom(replaceMethod.getParameterTypes()[0])) {
                params[0] = Collections.singleton(element);
            } else {
                params[0] = element;
            }
            if (paramLength > 1 && replaceMethod.getParameterTypes()[1].equals(Element.class)) {
                params[1] = element;
            }
            System.out.println("Invoking " + replaceMethod.getName());
            replaceMethod.invoke(cacheAccessor, params);
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
        Element element = new Element(key, "someValue");

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
