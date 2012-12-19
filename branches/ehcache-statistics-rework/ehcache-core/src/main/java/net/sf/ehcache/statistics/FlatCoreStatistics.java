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

package net.sf.ehcache.statistics;

public interface FlatCoreStatistics {

    // cache level
    long cacheHitCount();

    long cacheMissExpiredCount();

    long cacheMissNotFoundCount();

    long cacheMissCount(); // sum of prev 2

    long cachePutAddedCount();

    long cachePutUpdatedCount();

    long cachePutCount(); // sum of prev 2

    long cacheRemoveCount();

    // heap
    long localHeapHitCount();

    long localHeapMissCount();

    long localHeapPutAddedCount();

    long localHeapPutUpdatedCount();

    long localHeapPutCount(); // sum of prev 2

    long localHeapRemoveCount();

    // offheap
    long localOffHeapHitCount();

    long localOffHeapMissCount();

    long localOffHeapPutAddedCount();

    long localOffHeapPutUpdatedCount();

    long localOfHeapPutCount(); // sum of prev 2

    long localOffHeapRemoveCount();

    // disk
    long localDiskHitCount();

    long localDiskMissCount();

    long localDiskPutAddedCount();

    long localDiskPutUpdatedCount();

    long localDiskPutCount(); // sum of prev 2

    long localDiskRemoveCount();

    // xa
    long xaCommitReadOnlyCount();

    long xaCommitExceptionCount();

    long xaCommitCommittedCount();

    long xaCommitCount(); // sum of prev 3

    long xaRecoveryNothingCount();

    long xaRecoveryRecoveredCount();

    long xaRecoveryCount(); // sum of prev 2

    long xaRollbackExceptionCount();

    long xaRollbackSuccessCount();

    long xaRollbackCount(); // sum of prev 2

    // placeholder
    long cacheExpiredCount();

    long cacheEvictedCount();

}
