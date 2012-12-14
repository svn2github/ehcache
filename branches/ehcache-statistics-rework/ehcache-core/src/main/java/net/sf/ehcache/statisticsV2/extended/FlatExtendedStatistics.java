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

package net.sf.ehcache.statisticsV2.extended;

import java.util.concurrent.TimeUnit;

import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Result;


public interface FlatExtendedStatistics {

    void setStatisticsTimeToDisable(long time, TimeUnit unit);

    Result cacheGetOperation();
    Result cacheHitOperation();
    Result cacheMissExpiredOperation();
    Result cacheMissNotFoundOperation();
    Result cacheMissOperation();
    Result cachePutAddedOperation();
    Result cachePutReplacedOperation();
    Result cachePutOperation();
    Result cacheRemoveOperation();

    Result localHeapHitOperation();
    Result localHeapMissOperation();
    Result localHeapPutAddedOperation();
    Result localHeapPutReplacedOperation();
    Result localHeapPutOperation();
    Result localHeapRemoveOperation();

    Result localOffHeapHitOperation();
    Result localOffHeapMissOperation();
    Result localOffHeapPutAddedOperation();
    Result localOffHeapPutReplacedOperation();
    Result localOffHeapPutOperation();
    Result localOffHeapRemoveOperation();

    Result localDiskHitOperation();
    Result localDiskMissOperation();
    Result localDiskPutAddedOperation();
    Result localDiskPutReplacedOperation();
    Result localDiskPutOperation();
    Result localDiskRemoveOperation();

    Result cacheSearchOperation();

    Result xaCommitSuccessOperation();
    Result xaCommitExceptionOperation();
    Result xaCommitReadOnlyOperation();
    Result xaRollbackOperation();
    Result xaRollbackExceptionOperation();
    Result xaRecoveryOperation();

    Result cacheEvictionOperation();
    Result cacheExpiredOperation();

    // pass through stats
    long getSize();
    
    long getLocalHeapSize();

    long getLocalHeapSizeInBytes();

    long getLocalOffHeapSize();

    long getLocalOffHeapSizeInBytes();

    long getLocalDiskSize();

    long getLocalDiskSizeInBytes();

    long getWriterQueueLength();
}
