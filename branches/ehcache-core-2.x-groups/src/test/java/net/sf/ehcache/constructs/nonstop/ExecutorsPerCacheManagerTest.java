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

import java.util.List;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.ThreadDump.ThreadInformation;

public class ExecutorsPerCacheManagerTest extends TestCase {

    public void testExecutorsPerCacheManager() {
        CacheManager cacheManager1 = new CacheManager(getClass().getResourceAsStream("/nonstop/basic-cache-test.xml"));
        Cache cache = cacheManager1.getCache("test");
        NonStopCache nonStopCache1 = new NonStopCache(cache, "non-stop-test-cache");

        CacheManager cacheManager2 = new CacheManager(getClass().getResourceAsStream("/nonstop/ehcache-decorator-config-test.xml"));
        NonStopCache nonStopCache2 = (NonStopCache) cacheManager2.getEhcache("noopCacheDecorator");

        for (int i = 0; i < 1000; i++) {
            nonStopCache1.put(new Element("key" + i, "value" + i));
            nonStopCache2.put(new Element("key" + i, "value" + i));
        }

        List<ThreadInformation> threadDump = ThreadDump.getThreadDump();
        int count1 = 0;
        int count2 = 0;
        for (ThreadInformation info : threadDump) {
            if (info.getThreadName().contains(NonStopCacheExecutorService.EXECUTOR_THREAD_NAME_PREFIX)) {
                // System.out.println("Got executor thread: " + info.getThreadName());
                if (info.getThreadName().contains(cacheManager1.getName())) {
                    count1++;
                } else if (info.getThreadName().contains(cacheManager2.getName())) {
                    count2++;
                }
            }
        }
        assertEquals(NonStopCacheExecutorService.DEFAULT_MAX_THREAD_POOL_SIZE, count1);
        assertEquals(NonStopCacheExecutorService.DEFAULT_MAX_THREAD_POOL_SIZE, count2);
    }

    public void testMaxThreadPoolSizeProperty() {
        int maxThreads = 50;
        String theCacheManagerName = "someCacheManagerName";
        String maxThreadPoolSizePropertyPrefix = "net.sf.ehcache.constructs.nonstop.maxThreadPoolSize.";
        System.setProperty(maxThreadPoolSizePropertyPrefix + theCacheManagerName, "" + maxThreads);

        CacheManager cacheManager1 = new CacheManager(getClass().getResourceAsStream("/nonstop/ehcache-default-nonstop-decorator-test.xml"));
        Cache cache = cacheManager1.getCache("noDecoratorCache");
        NonStopCache nonStopCache1 = new NonStopCache(cache, "non-stop-test-cache");

        for (int i = 0; i < 1000; i++) {
            nonStopCache1.put(new Element("key" + i, "value" + i));
        }

        List<ThreadInformation> threadDump = ThreadDump.getThreadDump();
        int count1 = 0;
        for (ThreadInformation info : threadDump) {
            if (info.getThreadName().contains(NonStopCacheExecutorService.EXECUTOR_THREAD_NAME_PREFIX)) {
                // System.out.println("Got executor thread: " + info.getThreadName());
                if (info.getThreadName().contains(cacheManager1.getName())) {
                    count1++;
                }
            }
        }
        assertEquals(maxThreads, count1);
    }

}
