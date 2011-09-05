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

import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;
import net.sf.ehcache.store.TerracottaStore;

/**
 * Interface for resolving the active TerracottaStore, nonstop Executor Service, underlying {@link CacheLockProvider}, all of which can
 * change on rejoin
 *
 * @author Abhishek Sanoujam
 *
 */
public interface NonstopActiveDelegateHolder {

    /**
     * {@link TerracottaStore} got initialized again on rejoin
     *
     * @param newTerracottaStore
     */
    public void terracottaStoreInitialized(TerracottaStore newTerracottaStore);

    /**
     * Returns the current underlying {@link TerracottaStore}
     *
     * @return the current underlying {@link TerracottaStore}
     */
    public TerracottaStore getUnderlyingTerracottaStore();

    /**
     * Returns the current underlying NonstopExecutorService
     *
     * @return the current underlying NonstopExecutorService
     */
    public NonstopExecutorService getNonstopExecutorService();

    /**
     * Returns the current underlying {@link CacheLockProvider}
     *
     * @return the current underlying {@link CacheLockProvider}
     */
    public CacheLockProvider getUnderlyingCacheLockProvider();

    /**
     * Returns the nonstop store
     *
     * @return the nonstop store
     */
    public NonstopStore getNonstopStore();

}
