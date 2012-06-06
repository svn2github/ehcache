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

package net.sf.ehcache.constructs.nonstop;

import net.sf.ehcache.CacheManager;

/**
 * Factory for {@link NonstopExecutorService}
 *
 * @author Abhishek Sanoujam
 *
 */
public interface NonstopExecutorServiceFactory {

    /**
     * Creates a {@link NonstopExecutorService} and returns it if not already created
     *
     * @param cacheManager the {@link CacheManager}
     * @return A {@link NonstopExecutorService}
     */
    NonstopExecutorService getOrCreateNonstopExecutorService(CacheManager cacheManager);

    /**
     * Shuts down the {@link NonstopExecutorService} associated with the {@link CacheManager}
     *
     * @param cacheManager
     */
    void shutdown(CacheManager cacheManager);

}
