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

package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import java.util.Set;

/**
 * A copying cache store designed for transactional terracotta clustered stores
 *
 * @author ljacomet
 */
public final class TerracottaTransactionalCopyingCacheStore extends AbstractCopyingCacheStore<TerracottaStore> implements TerracottaStore {

    /**
     * Creates a terracotta copying instance of store, that wraps the actual storage
     *
     * @param store                the real store
     * @param copyStrategyInstance the copy strategy to use on every copy operation
     */
    public TerracottaTransactionalCopyingCacheStore(TerracottaStore store, ReadWriteCopyStrategy<Element> copyStrategyInstance) {
        super(store, true, false, copyStrategyInstance);
    }

    @Override
    public Element unsafeGet(Object key) {
        return getCopyStrategyHandler().copyElementForReadIfNeeded(getUnderlyingStore().unsafeGet(key));
    }

    @Override
    public void quickClear() {
        getUnderlyingStore().quickClear();
    }

    @Override
    public Set getLocalKeys() {
        return getUnderlyingStore().getLocalKeys();
    }

    @Override
    public CacheConfiguration.TransactionalMode getTransactionalMode() {
        return getUnderlyingStore().getTransactionalMode();
    }

    @Override
    public WriteBehind createWriteBehind() {
        return getUnderlyingStore().createWriteBehind();
    }
}
