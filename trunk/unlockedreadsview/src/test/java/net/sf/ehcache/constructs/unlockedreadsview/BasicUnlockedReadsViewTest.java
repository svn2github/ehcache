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

package net.sf.ehcache.constructs.unlockedreadsview;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaUnitTesting;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class BasicUnlockedReadsViewTest extends TestCase {

    public void testBasicUnlockedReadsView() throws Exception {
        ClusteredInstanceFactory mockFactory = Mockito.mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory);

        TerracottaStore mockStore = mock(TerracottaStore.class);
        when(mockFactory.createStore((Ehcache) any())).thenReturn(mockStore);

        final AtomicInteger unlockedGetCount = new AtomicInteger();
        final AtomicInteger unlockedGetQuietCount = new AtomicInteger();
        when(mockStore.unlockedGet(any())).thenAnswer(new Answer<Element>() {
            public Element answer(InvocationOnMock invocation) throws Throwable {
                unlockedGetCount.incrementAndGet();
                return null;
            }
        });
        when(mockStore.unlockedGetQuiet(any())).thenAnswer(new Answer<Element>() {
            public Element answer(InvocationOnMock invocation) throws Throwable {
                unlockedGetQuietCount.incrementAndGet();
                return null;
            }
        });

        CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/basic-unlocked-reads-view-test.xml"));
        Cache cache = cacheManager.getCache("test");
        Assert.assertNotNull(cache);

        UnlockedReadsView unlockedView = new UnlockedReadsView(cache, "unlocked-reads-view");

        final int N = 50;

        for (int i = 0; i < N; i++) {
            unlockedView.get("some-key");
        }
        for (int i = 0; i < N; i++) {
            unlockedView.getQuiet("some-key");
        }

        Assert.assertEquals(N, unlockedGetCount.get());
        Assert.assertEquals(N, unlockedGetQuietCount.get());
    }
}
