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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(value = MockitoJUnitRunner.class)
public class BasicNonstopTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(BasicNonstopTest.class);

    private final List<Method> methods = NonstopTestUtil.getEhcacheMethodsTouchingStore();

    @Test
    public void testTimeout() throws Exception {
        LOG.info("Testing timeout with all timeoutBehaviors");
        doTestExceptionOnTimeout();
        doTestNoopOnTimeout();
        doTestLocalReadsOnTimeout();
        LOG.info("Test finished successfully!");
    }

    private void doGenericTest(EhcacheMethodsInvoker invoker, NonstopConfiguration nonstopConfig) throws Exception {
        BlockingMockStore blockingMockStore = new BlockingMockStore();
        blockingMockStore.setBlocking(false);
        Cache cache = NonstopTestUtil.getMockTerracottaStore(blockingMockStore, nonstopConfig);
        blockingMockStore.setBlocking(true);

        invoker.invokeAll(cache);
    }

    private void doTestExceptionOnTimeout() throws Exception {
        LOG.info("Running EXCEPTION on timeout test");
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
                            LOG.info(" Caught expected exception: " + cause);
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
        LOG.info("Running NOOP on timeout test");
        EhcacheMethodsInvoker invoker = new EhcacheMethodsInvoker() {
            @Override
            protected void invokeOne(Ehcache ehcache, Method method) {
                if (methods.contains(method)) {
                    System.out.print("Invoking method: " + NonstopTestUtil.getMethodSignature(method) + " ...");
                    super.invokeOne(ehcache, method);
                    LOG.info(" succeeded with no-op.");
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
        LOG.info("Running LocalReads on timeout test");
        EhcacheMethodsInvoker invoker = new EhcacheMethodsInvoker() {
            @Override
            protected void invokeOne(Ehcache ehcache, Method method) {
                if (methods.contains(method)) {
                    LOG.info("Invoking method: " + NonstopTestUtil.getMethodSignature(method) + " ...");
                    super.invokeOne(ehcache, method);
                    LOG.info(" ... succeeded with localReads");
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
