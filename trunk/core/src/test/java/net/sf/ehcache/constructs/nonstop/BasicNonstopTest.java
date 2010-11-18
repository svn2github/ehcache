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

import java.lang.reflect.Method;
import java.util.List;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.NonstopTestUtil.EhcacheMethodsInvoker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(value = MockitoJUnitRunner.class)
public class BasicNonstopTest extends TestCase {

    private final List<Method> methods = NonstopTestUtil.getEhcacheMethodsTouchingStore();

    @Test
    public void testTimeout() throws Exception {
        System.out.println("Testing timeout with all timeoutBehaviors");
        doTestExceptionOnTimeout();
        doTestNoopOnTimeout();
        doTestLocalReadsOnTimeout();
        System.out.println("Test finished successfully!");
    }

    private void doGenericTest(EhcacheMethodsInvoker invoker, NonstopConfiguration nonstopConfig) throws Exception {
        BlockingMockStore blockingMockStore = new BlockingMockStore();
        blockingMockStore.setBlocking(false);
        Cache cache = NonstopTestUtil.getMockTerracottaStore(blockingMockStore, nonstopConfig);
        blockingMockStore.setBlocking(true);

        invoker.invokeAll(cache);
    }

    private void doTestExceptionOnTimeout() throws Exception {
        System.out.println("Running EXCEPTION on timeout test");
        EhcacheMethodsInvoker invoker = new EhcacheMethodsInvoker() {
            @Override
            protected void invokeOne(Ehcache ehcache, Method method) {
                if (methods.contains(method)) {
                    System.out.print("Invoking method: " + NonstopTestUtil.getMethodSignature(method) + " ...");
                    try {
                        super.invokeOne(ehcache, method);
                        fail("Invoking method '" + NonstopTestUtil.getMethodSignature(method) + "' should have thrown exception");
                    } catch (Exception e) {
                        Throwable cause = getRootCause(e);
                        if (cause instanceof NonStopCacheException) {
                            System.out.println(" Caught expected exception: " + cause);
                        } else {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

        };

        NonstopConfiguration nonstopConfig = new NonstopConfiguration();
        nonstopConfig.enabled(true);
        nonstopConfig.getTimeoutBehavior().setType("exception");
        nonstopConfig.setTimeoutMillis(100);

        doGenericTest(invoker, nonstopConfig);
    }

    private static Throwable getRootCause(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

    private void doTestNoopOnTimeout() throws Exception {
        System.out.println("Running NOOP on timeout test");
        EhcacheMethodsInvoker invoker = new EhcacheMethodsInvoker() {
            @Override
            protected void invokeOne(Ehcache ehcache, Method method) {
                if (methods.contains(method)) {
                    System.out.print("Invoking method: " + NonstopTestUtil.getMethodSignature(method) + " ...");
                    super.invokeOne(ehcache, method);
                    System.out.println(" succeeded with no-op.");
                }
            }

        };

        NonstopConfiguration nonstopConfig = new NonstopConfiguration();
        nonstopConfig.enabled(true);
        nonstopConfig.getTimeoutBehavior().setType("noop");
        nonstopConfig.setTimeoutMillis(100);

        doGenericTest(invoker, nonstopConfig);
    }

    private void doTestLocalReadsOnTimeout() throws Exception {
        System.out.println("Running LocalReads on timeout test");
        EhcacheMethodsInvoker invoker = new EhcacheMethodsInvoker() {
            @Override
            protected void invokeOne(Ehcache ehcache, Method method) {
                if (methods.contains(method)) {
                    System.out.print("Invoking method: " + NonstopTestUtil.getMethodSignature(method) + " ...");
                    try {
                        super.invokeOne(ehcache, method);
                    } catch (Exception e) {
                        Throwable rootCause = getRootCause(e);
                        if (rootCause instanceof IllegalArgumentException
                                && rootCause.getMessage().contains(
                                        "LocalReadsOnTimeoutStore can be only be used with Terracotta clustered caches")) {
                            System.out.println(" Caught expected exception: localReads supported only for terracotta clustered caches");
                        }
                    }
                }
            }

        };

        NonstopConfiguration nonstopConfig = new NonstopConfiguration();
        nonstopConfig.enabled(true);
        nonstopConfig.getTimeoutBehavior().setType("localReads");
        nonstopConfig.setTimeoutMillis(100);

        doGenericTest(invoker, nonstopConfig);
    }
}
