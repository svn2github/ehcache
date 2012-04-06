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

package net.sf.ehcache;

import net.sf.ehcache.CacheManager.CacheRejoinAction;
import net.sf.ehcache.constructs.nonstop.CacheManagerExecutorServiceFactory;
import net.sf.ehcache.constructs.nonstop.NonstopExecutorService;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.ClusteredInstanceFactoryWrapper;

import org.mockito.Mockito;

public class CacheManagerMockHelper {

    public static void mockGetNonstopExecutorService(CacheManager cacheManager) {
        NonstopExecutorService executorService = CacheManagerExecutorServiceFactory.getInstance().getOrCreateNonstopExecutorService(cacheManager);
        Mockito.when(cacheManager.getNonstopExecutorService()).thenReturn(executorService);
    }

    public static void mockGetClusteredInstanceFactory(CacheManager cacheManager, Cache cache) {
        ClusteredInstanceFactory clusteredInstanceFactory = Mockito.mock(ClusteredInstanceFactoryWrapper.class);
        Mockito.when(cacheManager.getClusteredInstanceFactory(cache)).thenReturn(clusteredInstanceFactory);
    }

    public static void mockGetCacheRejoinAction(CacheManager cacheManager) {
        CacheRejoinAction cacheRejoinAction = Mockito.mock(CacheRejoinAction.class);
        Mockito.when(cacheManager.getCacheRejoinAction()).thenReturn(cacheRejoinAction);
    }

}
