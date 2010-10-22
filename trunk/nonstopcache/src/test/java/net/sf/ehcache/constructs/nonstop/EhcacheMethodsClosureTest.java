/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.constructs.nonstop;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.store.Store;

import org.junit.Test;

/**
 * Test all methods in net.sf.ehcache.Ehcache leading upto the net.sf.ehcache.store.Store are handled by {@link NonStopCacheBehaviorType}
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class EhcacheMethodsClosureTest extends TestCase {

    private static final Set<String> NON_STOP_CACHE_BEHAVIOR_METHODS = new HashSet<String>();
    static {
        for (Method m : NonStopCacheBehavior.class.getMethods()) {
            NON_STOP_CACHE_BEHAVIOR_METHODS.add(getMethodSignature(m));
        }
    }
    private CacheManager cacheManager;

    private static String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append('(');

        Class[] parameterTypes = method.getParameterTypes();
        for (int j = 0; j < parameterTypes.length; j++) {
            sb.append(parameterTypes[j].getName());
            if (j < (parameterTypes.length - 1)) {
                sb.append(',');
            }
        }
        sb.append(')');
        return sb.toString();
    }

    private Object[] getMethodArguments(Method m) throws Exception {
        Class<?>[] parameterTypes = m.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameters.length; i++) {
            try {
                parameters[i] = getArgumentInstanceFor(m, parameterTypes[i]);
            } catch (InstantiationException e) {
                throw e;
            } catch (IllegalAccessException e) {
                throw e;
            }
        }
        return parameters;
    }

    private Object getArgumentInstanceFor(Method method, Class<?> parameterClass) throws Exception {
        if (parameterClass.equals(Object.class) || parameterClass.equals(Serializable.class)) {
            // isKeyInCache doesn't touch tc layer for null keys in LocalReadsBehavior
            return "SomeKey";
        } else if (parameterClass.equals(Element.class)) {
            // put calls with null does not touch terracotta layer... use non-null arg
            return new Element("someKey", "someValue");
        } else if (parameterClass.equals(Boolean.TYPE)) {
            return Boolean.FALSE;
        } else if ((method.getName().equals("getSizeBasedOnAccuracy") || method.getName().equals("setStatisticsAccuracy"))
                && parameterClass.equals(Integer.TYPE)) {
            return Statistics.STATISTICS_ACCURACY_BEST_EFFORT;
        } else {
            String msg = "Unhandled parameter type for method: " + getMethodSignature(method) + ", type: " + parameterClass.getName();
            throw new Exception(msg);
        }
    }

    @Test
    public void testClosure() throws Exception {
        cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-cache-test.xml"));
        Ehcache cache = cacheManager.getEhcache("test");

        final MockStoreWithFlag mockStore = new MockStoreWithFlag();
        replaceStoreField((Cache) cache, mockStore);

        final Set<String> skipMethods = new HashSet<String>();
        skipMethods.add("setCacheManager");
        skipMethods.add("setDiskStorePath");
        skipMethods.add("setTransactionManagerLookup");
        skipMethods.add("initialise");
        skipMethods.add("registerCacheUsageListener");
        skipMethods.add("removeCacheUsageListener");
        skipMethods.add("setBootstrapCacheLoader");
        skipMethods.add("registerCacheExtension");
        skipMethods.add("unregisterCacheExtension");
        skipMethods.add("registerCacheLoader");
        skipMethods.add("unregisterCacheLoader");
        skipMethods.add("registerCacheWriter");
        skipMethods.add("unregisterCacheWriter");
        skipMethods.add("setDisabled");
        // should this be uncommented? [start[
        skipMethods.add("getWithLoader");
        skipMethods.add("getAllWithLoader");
        skipMethods.add("loadAll");
        skipMethods.add("isClusterCoherent");
        skipMethods.add("isNodeCoherent");
        // ]end]
        skipMethods.add("setNodeCoherent");
        skipMethods.add("waitUntilClusterCoherent");
        skipMethods.add("clone");
        skipMethods.add("setName");
        skipMethods.add("dispose");
        skipMethods.add("setCacheExceptionHandler");
        skipMethods.add("addPropertyChangeListener");
        skipMethods.add("removePropertyChangeListener");
        // off-heap methods don't reach terracotta layer.. yet
        skipMethods.add("calculateOffHeapSize");
        skipMethods.add("getOffHeapStoreSize");
        // search methods 
        skipMethods.add("getSearchAttribute");
        new EhcacheMethodsInvoker() {

            @Override
            protected void invokeOne(Ehcache ehcache, Method m) throws Exception {
                mockStore.clearAccessFlag();
                if (skipMethods.contains(m.getName())) {
                    System.out.println(" Skipped: " + getMethodSignature(m));
                    return;
                }
                System.out.print("Invoking: " + getMethodSignature(m) + " ... ");
                super.invokeOne(ehcache, m);
                if (mockStore.isAccessFlagMarked()) {
                    checkMethodPresent(m, mockStore);
                    System.out.println(" ... reached Store layer at Store." + mockStore.getLastMethodInvoked() + "(..)");
                } else {
                    System.out.println(" ... didn't reach Store layer.");
                }
            }

        }.invokeAll(cache);

        System.out.println("Finished test successfully");
    }

    private void checkMethodPresent(Method method, MockStoreWithFlag mockStore) {
        String methodSignature = getMethodSignature(method);
        if (!NON_STOP_CACHE_BEHAVIOR_METHODS.contains(methodSignature)) {
            String msg = "NonStopCacheBehavior should also have this method as it reaches the Store [at Store."
                    + mockStore.getLastMethodInvoked() + "(..)] -- " + getMethodSignature(method);
            throw new AssertionError(msg);
        }
    }

    private void replaceStoreField(Cache cache, Store replaceWith) throws Exception {
        Field storeField = Cache.class.getDeclaredField("compoundStore");
        storeField.setAccessible(true);
        storeField.set(cache, replaceWith);
    }

    private class EhcacheMethodsInvoker {

        public void invokeAll(Ehcache ehcache) throws Exception {
            Method[] methods = Ehcache.class.getMethods();
            for (Method m : methods) {
                invokeOne(ehcache, m);
            }
        }

        protected void invokeOne(Ehcache ehcache, Method m) throws Exception {
            Object[] args = getMethodArguments(m);
            m.invoke(ehcache, args);
        }

    }

}
