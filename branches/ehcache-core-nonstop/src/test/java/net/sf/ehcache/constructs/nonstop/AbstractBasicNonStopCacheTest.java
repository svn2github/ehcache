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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.store.Store;

import org.junit.Assert;

public abstract class AbstractBasicNonStopCacheTest extends TestCase {

    private static final int SYSTEM_CLOCK_EPSILON_MILLIS = 90;

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

    private static Object[] getMethodArguments(Method m) throws Exception {
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

    private static Object getArgumentInstanceFor(Method method, Class<?> parameterClass) throws Exception {
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

    protected void doTestForCacheTimeout(Cache cache, long timeoutMillis, NonStopCache nonStopCache) throws Exception {
        // default is exception on timeout
        doExceptionOnTimeout(cache, timeoutMillis, nonStopCache);

        // test null-op
        nonStopCache.setTimeoutBehaviorType(NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        doNoopTest(cache, timeoutMillis, nonStopCache);

        // localReadsBehavior -- only works when clustered
        nonStopCache.setTimeoutBehaviorType(NonStopCacheBehaviorType.LOCAL_READS_ON_TIMEOUT);
        doLocalReadsTest(cache, timeoutMillis, nonStopCache);
    }

    private void configureTimeout(long timeoutMillis, NonStopCache nonStopCache) {
        System.out.println("Configuring cache with timeoutMillis of: " + timeoutMillis);
        nonStopCache.getNonStopCacheConfig().setTimeoutMillis(timeoutMillis);
    }

    private void replaceWithBlockingStore(Cache cache) throws Exception {
        BlockingMockStore mockClusteredStore = new BlockingMockStore();
        replaceStoreField(cache, mockClusteredStore);
    }

    protected void doLocalReadsTest(Cache cache, long timeoutMillis, NonStopCache nonStopCache) throws Exception {
        replaceWithBlockingStore(cache);
        configureTimeout(timeoutMillis, nonStopCache);
        System.out.println("########## Testing LocalReadsBehavior ");
        new NonStopCacheBehaviorInvoker(timeoutMillis) {

            @Override
            protected void invokeOne(NonStopCacheBehavior behavior, Method method, Object[] args) throws IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException {
                try {
                    super.invokeOne(behavior, method, args);
                    fail("NonStopCache's configured with local reads behavior should work only with Terracotta");
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof IllegalArgumentException) {
                        System.out.println("   ... caught expected exception (local reads behavior only works with Terracotta).");
                    } else {
                        throw e;
                    }
                }
            }

        }.invokeAll(nonStopCache);
    }

    protected void doNoopTest(Cache cache, long timeoutMillis, NonStopCache nonStopCache) throws Exception {
        System.out.println("########## Testing NoopBehavior ");
        replaceWithBlockingStore(cache);
        configureTimeout(timeoutMillis, nonStopCache);
        new NonStopCacheBehaviorInvoker(timeoutMillis).invokeAll(nonStopCache);
    }

    protected void doExceptionOnTimeout(Cache cache, long timeoutMillis, NonStopCache nonStopCache) throws Exception {
        System.out.println("########## Testing ExceptionOnTimeoutBehavior ");
        replaceWithBlockingStore(cache);
        configureTimeout(timeoutMillis, nonStopCache);
        // check for default behavior -- timeout behavior
        new NonStopCacheBehaviorInvoker(timeoutMillis) {

            @Override
            protected void invokeOne(NonStopCacheBehavior behavior, Method method, Object[] args) throws IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException {
                try {
                    super.invokeOne(behavior, method, args);
                    fail("NonStopCache's configured with timeout behavior should throw NonStopCacheException - " + method.getName());
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof NonStopCacheException) {
                        System.out.println("   ... caught expected exception -- " + e.getCause());
                    } else {
                        throw e;
                    }
                }
            }

        }.invokeAll(nonStopCache);
    }

    private void replaceStoreField(Cache cache, Store replaceWith) throws Exception {
        Field storeField = Cache.class.getDeclaredField("compoundStore");
        storeField.setAccessible(true);
        storeField.set(cache, replaceWith);
    }

    private static class NonStopCacheBehaviorInvoker {

        private final long underlyingCacheTimeout;
        private final List<String> skipMethods = new ArrayList<String>();

        public NonStopCacheBehaviorInvoker(long underlyingCacheTimeout) {
            this.underlyingCacheTimeout = underlyingCacheTimeout;
            skipMethods.add("isElementOnDisk");
        }

        public void invokeAll(NonStopCacheBehavior behavior) throws Exception {
            // make sure all methods returns and does not block
            Method[] methods = NonStopCacheBehavior.class.getMethods();
            for (Method m : methods) {
                if (skipMethods.contains(m.getName())) {
                    System.out.println("Skipped: " + m.getName());
                    return;
                }
                long start = System.currentTimeMillis();
                System.out.println("Invoking method: " + getMethodSignature(m) + "... ");
                invokeOne(behavior, m, getMethodArguments(m));
                long timeTaken = System.currentTimeMillis() - start;
                System.out.println("      ... underlying cacheTimeout: " + underlyingCacheTimeout + ", actual time taken: " + timeTaken);
                Assert.assertTrue("Method " + m.getName() + " should have taken at least " + underlyingCacheTimeout + ". Actual: "
                        + timeTaken, (timeTaken + SYSTEM_CLOCK_EPSILON_MILLIS) >= underlyingCacheTimeout);
            }
        }

        protected void invokeOne(NonStopCacheBehavior behavior, Method m, Object[] args) throws InvocationTargetException,
                IllegalArgumentException, IllegalAccessException {
            m.invoke(behavior, args);
        }

    }

}
