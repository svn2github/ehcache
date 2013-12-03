/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

import java.text.MessageFormat;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CopyOnReadAndTTITest {
    private CacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        cacheManager = new CacheManager().create();
    }

    @After
    public void tearDown() throws Exception {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
    }

    @Test
    public void test() {
        String key = "key";

        // original example had these true: copyOnWrite(true).statistics(true).logging(true)
        CacheConfiguration config = new CacheConfiguration().name("copyOnReadTest").timeToIdleSeconds(20).timeToLiveSeconds(30)
                .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU).copyOnRead(true).maxBytesLocalHeap(100, MemoryUnit.KILOBYTES);

        Cache cache = new Cache(config);
        cacheManager.addCache(cache);

        long start = System.currentTimeMillis();
        cache.put(new Element(key, "test"));
        for (int times = 0; times < 50; times++) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            

            Element e = cache.get(key);
            long elapsed = System.currentTimeMillis() - start;
            if (e != null) {
                System.out.println(MessageFormat.format("{0}ms - Creation: {1}, Access - {2}, Update: {3}, TTI: {4}, TTL: {5}, expired at: {6}, calculated TTL: {7}", elapsed, e.getCreationTime(),
                        e.getLastAccessTime(), e.getLastUpdateTime(), e.getTimeToIdle(), e.getTimeToLive(), e.getExpirationTime(), (e.getExpirationTime()-e.getCreationTime())/1000L));
            } else {
                System.out.println(MessageFormat.format("{0}ms - Expired", elapsed));
                break;
            }
        }
        long millisToExpire = System.currentTimeMillis() - start;
        assertThat(millisToExpire, greaterThanOrEqualTo(config.getTimeToLiveSeconds() * 1000));
    }
}
