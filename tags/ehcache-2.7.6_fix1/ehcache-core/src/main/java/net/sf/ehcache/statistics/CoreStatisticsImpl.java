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

import java.util.Arrays;
import java.util.EnumSet;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.ClusterEventOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;
import net.sf.ehcache.CacheOperationOutcomes.ExpiredOutcome;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

/**
 * The CoreStatisticsImpl class.
 *
 * @author cschanck
 */
public class CoreStatisticsImpl implements CoreStatistics {

    private final ExtendedStatistics extended;
    private final CountOperation cacheGet;
    private final CountOperation cachePut;
    private final CountOperation cacheRemove;
    private final CountOperation localHeapGet;
    private final CountOperation localHeapPut;

    private final CountOperation localHeapRemove;
    private final CountOperation localOffHeapGet;
    private final CountOperation localOffHeapPut;
    private final CountOperation localOffHeapRemove;
    private final CountOperation localDiskGet;
    private final CountOperation localDiskPut;
    private final CountOperation localDiskRemove;
    private final CountOperation xaCommit;
    private final CountOperation xaRecovery;
    private final CountOperation xaRollback;
    private final CountOperation evicted;
    private final CountOperation expired;

    private final CountOperation cacheClusterEvent;

    /**
     * Instantiates a new core statistics impl.
     *
     * @param extended the extended
     */
    public CoreStatisticsImpl(ExtendedStatistics extended) {
        this.extended = extended;
        this.cacheGet = asCountOperation(extended.get());
        this.cachePut = asCountOperation(extended.put());
        this.cacheRemove = asCountOperation(extended.remove());

        this.localHeapGet = asCountOperation(extended.heapGet());
        this.localHeapPut = asCountOperation(extended.heapPut());
        this.localHeapRemove = asCountOperation(extended.heapRemove());

        this.localOffHeapGet = asCountOperation(extended.offheapGet());
        this.localOffHeapPut = asCountOperation(extended.offheapPut());
        this.localOffHeapRemove = asCountOperation(extended.offheapRemove());

        this.localDiskGet = asCountOperation(extended.diskGet());
        this.localDiskPut = asCountOperation(extended.diskPut());
        this.localDiskRemove = asCountOperation(extended.diskRemove());

        this.xaCommit = asCountOperation(extended.xaCommit());
        this.xaRecovery = asCountOperation(extended.xaRecovery());
        this.xaRollback = asCountOperation(extended.xaRollback());

        this.evicted = asCountOperation(extended.eviction());
        this.expired = asCountOperation(extended.expiry());

        this.cacheClusterEvent = asCountOperation(extended.clusterEvent());
    }

    private static <T extends Enum<T>> CountOperation asCountOperation(final Operation<T> compoundOp) {
        return new CountOperation<T>() {
            @Override
            public long value(T result) {
                return compoundOp.component(result).count().value();
            }

            @Override
            public long value(T... results) {
                return compoundOp.compound(EnumSet.copyOf(Arrays.asList(results))).count().value();
            }

        };
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#get()
     */
    @Override
    public CountOperation<CacheOperationOutcomes.GetOutcome> get() {
        return cacheGet;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#put()
     */
    @Override
    public CountOperation<CacheOperationOutcomes.PutOutcome> put() {
        return cachePut;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#remove()
     */
    @Override
    public CountOperation<CacheOperationOutcomes.RemoveOutcome> remove() {
        return cacheRemove;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localHeapGet()
     */
    @Override
    public CountOperation<GetOutcome> localHeapGet() {
        return localHeapGet;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localHeapPut()
     */
    @Override
    public CountOperation<PutOutcome> localHeapPut() {
        return localHeapPut;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localHeapRemove()
     */
    @Override
    public CountOperation<RemoveOutcome> localHeapRemove() {
        return localHeapRemove;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localOffHeapGet()
     */
    @Override
    public CountOperation<GetOutcome> localOffHeapGet() {
        return localOffHeapGet;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localOffHeapPut()
     */
    @Override
    public CountOperation<PutOutcome> localOffHeapPut() {
        return localOffHeapPut;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localOffHeapRemove()
     */
    @Override
    public CountOperation<RemoveOutcome> localOffHeapRemove() {
        return localOffHeapRemove;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localDiskGet()
     */
    @Override
    public CountOperation<GetOutcome> localDiskGet() {
        return localDiskGet;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localDiskPut()
     */
    @Override
    public CountOperation<PutOutcome> localDiskPut() {
        return localDiskPut;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statistics.CoreStatistics#localDiskRemove()
     */
    @Override
    public CountOperation<RemoveOutcome> localDiskRemove() {
        return localDiskRemove;
    }

    /**
     * {@inheritDoc}
     */
    public CountOperation<XaCommitOutcome> xaCommit() {
        return xaCommit;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public CountOperation<XaRecoveryOutcome> xaRecovery() {
        return xaRecovery;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CountOperation<XaRollbackOutcome> xaRollback() {
        return xaRollback;
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public CountOperation<EvictionOutcome> cacheEviction() {
        return evicted;
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public CountOperation<ExpiredOutcome> cacheExpiration() {
        return expired;
    }

    @Override
    public CountOperation<ClusterEventOutcomes> cacheClusterEvent() {
        return cacheClusterEvent;
    }

}