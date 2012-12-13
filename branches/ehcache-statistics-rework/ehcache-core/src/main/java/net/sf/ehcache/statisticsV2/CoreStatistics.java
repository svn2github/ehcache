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

package net.sf.ehcache.statisticsV2;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.statisticsV2.CoreStatistics.CountOperation;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

public interface CoreStatistics {

    public interface CountOperation<T> {
        long value(T result);

        long value(T ... results);
    }

    public CountOperation<CacheOperationOutcomes.GetOutcome> get();
    public CountOperation<CacheOperationOutcomes.PutOutcome> put();
    public CountOperation<CacheOperationOutcomes.RemoveOutcome> remove();

    public CountOperation<StoreOperationOutcomes.GetOutcome> localHeapGet();
    public CountOperation<StoreOperationOutcomes.PutOutcome> localHeapPut();
    public CountOperation<StoreOperationOutcomes.RemoveOutcome> localHeapRemove();

    public CountOperation<StoreOperationOutcomes.GetOutcome> localOffHeapGet();
    public CountOperation<StoreOperationOutcomes.PutOutcome> localOffHeapPut();
    public CountOperation<StoreOperationOutcomes.RemoveOutcome> localOffHeapRemove();

    public CountOperation<StoreOperationOutcomes.GetOutcome> diskGet();
    public CountOperation<StoreOperationOutcomes.PutOutcome> diskPut();
    public CountOperation<StoreOperationOutcomes.RemoveOutcome> diskRemove();

    public CountOperation<XaCommitOutcome> xaCommit();
    public CountOperation<XaRecoveryOutcome> xaRecovery();
    public CountOperation<XaRollbackOutcome> xaRollback();

    // TBD hated CRSS
    public CountOperation<CacheOperationOutcomes.EvictionOutcome> cacheEviction();
    public CountOperation<CacheOperationOutcomes.ExpiredOutcome> cacheExpiration();


}