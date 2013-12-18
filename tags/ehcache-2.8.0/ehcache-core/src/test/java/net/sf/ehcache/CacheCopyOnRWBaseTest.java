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

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class CacheCopyOnRWBaseTest {

    public static final String NO_COPY_CACHE = "noCopyCache";
    public static final String COPY_ON_R_CACHE = "copyOnRCache";
    public static final String COPY_ON_W_CACHE = "copyOnWCache";
    public static final String COPY_ON_RW_CACHE = "copyOnRWCache";

    @Parameters(name = "Persistence: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = { { PersistenceConfiguration.Strategy.NONE },
                { PersistenceConfiguration.Strategy.LOCALTEMPSWAP } };
        return Arrays.asList(data);
    }

    private final PersistenceConfiguration.Strategy strategy;

    private CacheManager cacheManager;

    public CacheCopyOnRWBaseTest(PersistenceConfiguration.Strategy strategy) {
        this.strategy = strategy;
    }

    @Before
    public void setUp() throws Exception {
        cacheManager = CacheManager.create( new Configuration()
                .name("copyOnRWBaseManager")
                .diskStore(new DiskStoreConfiguration().path(System.getProperty("java.io.tmpdir")))
                .maxBytesLocalHeap(100, MemoryUnit.KILOBYTES)
                .maxBytesLocalDisk(200, MemoryUnit.KILOBYTES));
    }

    @After
    public void tearDown() {
        cacheManager.shutdown();
    }

    @Test
    public void testNoCopyCache() {
        cacheManager.addCache(new Cache(new CacheConfiguration().name(NO_COPY_CACHE)
                .persistence(new PersistenceConfiguration().strategy(strategy))));

        Cache cache = cacheManager.getCache(NO_COPY_CACHE);
        ValueHolder valueHolder = new ValueHolder("value");
        String key = "key";
        Element element = new Element(key, valueHolder, true);
        cache.put(element);

        String otherValue = "otherValue";
        valueHolder.value = otherValue;

        Element cacheContent = cache.get(key);
        assertThat(cacheContent, sameInstance(element));
        assertThat(((ValueHolder)cacheContent.getObjectValue()).value, is(otherValue));
    }

    @Test
    public void testCopyOnWCache() {
        cacheManager.addCache(new Cache(new CacheConfiguration().name(COPY_ON_W_CACHE)
                .persistence(new PersistenceConfiguration().strategy(strategy))
                .copyOnWrite(true)));

        Cache cache = cacheManager.getCache(COPY_ON_W_CACHE);
        ValueHolder valueHolder = new ValueHolder("value");
        String key = "key";
        Element element = new Element(key, valueHolder, true);
        cache.put(element);

        valueHolder.value = "otherValue";

        Element cacheContent = cache.get(key);
        assertThat(cacheContent, not(sameInstance(element)));
        assertThat(cacheContent, sameInstance(cache.get(key)));
        assertThat(((ValueHolder)cacheContent.getObjectValue()).value, is("value"));
    }

    @Test
    public void testCopyOnRWCache() {
        cacheManager.addCache(new Cache(new CacheConfiguration().name(COPY_ON_RW_CACHE)
                .persistence(new PersistenceConfiguration().strategy(strategy))
                .copyOnRead(true)
                .copyOnWrite(true)));

        Cache cache = cacheManager.getCache(COPY_ON_RW_CACHE);
        ValueHolder valueHolder = new ValueHolder("value");
        String key = "key";
        Element element = new Element(key, valueHolder, true);
        cache.put(element);

        valueHolder.value = "otherValue";

        Element cacheContent = cache.get(key);
        assertThat(cacheContent, not(sameInstance(element)));
        assertThat(cacheContent, not(sameInstance(cache.get(key))));
        assertThat(((ValueHolder)cacheContent.getObjectValue()).value, is("value"));
    }

    @Test
    public void testCopyOnRCache() {
        cacheManager.addCache(new Cache(new CacheConfiguration().name(COPY_ON_R_CACHE)
                .persistence(new PersistenceConfiguration().strategy(strategy))
                .copyOnRead(true)));

        Cache cache = cacheManager.getCache(COPY_ON_R_CACHE);
        ValueHolder valueHolder = new ValueHolder("value");
        String key = "key";
        Element element = new Element(key, valueHolder, true);
        cache.put(element);

        String otherValue = "otherValue";
        valueHolder.value = otherValue;

        Element cacheContent = cache.get(key);
        assertThat(cacheContent, not(sameInstance(element)));
        assertThat(((ValueHolder)cacheContent.getObjectValue()).value, is(otherValue));
    }

    private static class ValueHolder implements Serializable {
        String value;

        ValueHolder(String value) {
            this.value = value;
        }
    }
}
