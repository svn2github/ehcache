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

package net.sf.ehcache.constructs.nonstop.concurrency;

import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;

/**
 *
 * Class implementing {@link CacheLockProvider} with nonstop feature
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopCacheLockProvider implements CacheLockProvider {

    private final NonstopStore nonstopStore;
    private final NonstopActiveDelegateHolder nonstopActiveDelegateHolder;
    private final ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal;
    private final NonstopConfiguration nonstopConfiguration;

    /**
     * Public constructor
     *
     * @param nonstopStore
     * @param nonstopActiveDelegateHolder
     * @param explicitLockingContextThreadLocal
     */
    public NonstopCacheLockProvider(NonstopStore nonstopStore, NonstopActiveDelegateHolder nonstopActiveDelegateHolder,
            ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal, NonstopConfiguration nonstopConfiguration) {
        this.nonstopStore = nonstopStore;
        this.nonstopActiveDelegateHolder = nonstopActiveDelegateHolder;
        this.explicitLockingContextThreadLocal = explicitLockingContextThreadLocal;
        this.nonstopConfiguration = nonstopConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    public Sync getSyncForKey(Object key) {
        return new NonstopSync(nonstopStore, nonstopActiveDelegateHolder, explicitLockingContextThreadLocal, key, nonstopConfiguration);
    }
}
