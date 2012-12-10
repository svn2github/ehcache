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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheOperationOutcomes.GetOutcome;
import net.sf.ehcache.CacheOperationOutcomes.PutOutcome;
import net.sf.ehcache.CacheOperationOutcomes.RemoveOutcome;
import net.sf.ehcache.CacheOperationOutcomes.SearchOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes;
import net.sf.ehcache.transaction.xa.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.XaRollbackOutcome;

import org.terracotta.context.TreeNode;
import org.terracotta.statistics.SourceStatistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;

import static java.util.concurrent.TimeUnit.*;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;
import static org.terracotta.context.query.Matchers.*;

public class ExtendedStatisticsImpl implements ExtendedStatistics {
    
    /**
     * The default interval in seconds for the {@link SampledRateCounter} for recording the average search rate counter
     */
    public static int DEFAULT_SEARCH_INTERVAL_SECS = 10;

    /**
     * The default history size for {@link SampledCounter} objects.
     */
    public static int DEFAULT_HISTORY_SIZE = 30;

    /**
     * The default interval for sampling events for {@link SampledCounter} objects.
     */
    public static int DEFAULT_INTERVAL_SECS = 1;

    private final CompoundOperationImpl<GetOutcome> getCompound;
    private final CompoundOperationImpl<PutOutcome> putCompound;
    private final CompoundOperationImpl<RemoveOutcome> removeCompound;
    private final CompoundOperationImpl<?> evictedCompound;
    private final CompoundOperationImpl<?> expiredCompound;
    private final CompoundOperationImpl<StoreOperationOutcomes.GetOutcome> heapGetCompound;
    private final CompoundOperationImpl<StoreOperationOutcomes.GetOutcome> offheapGetCompound;
    private final CompoundOperationImpl<StoreOperationOutcomes.GetOutcome> diskGetCompound;
    private final CompoundOperationImpl<SearchOutcome> searchCompound;
    private final CompoundOperationImpl<XaCommitOutcome> xaCommitCompound;
    private final CompoundOperationImpl<XaRollbackOutcome> xaRollbackCompound;
    
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    
    public ExtendedStatisticsImpl(StatisticsManager manager) {
        getCompound = new CompoundOperationImpl(extractCacheStat(manager, "get"), GetOutcome.class, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        putCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), PutOutcome.class, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        removeCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), RemoveOutcome.class, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        evictedCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), null, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        expiredCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), null, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        
        heapGetCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), StoreOperationOutcomes.GetOutcome.class, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        offheapGetCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), StoreOperationOutcomes.GetOutcome.class, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        diskGetCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), StoreOperationOutcomes.GetOutcome.class, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        xaCommitCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), XaCommitOutcome.class, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);
        xaRollbackCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), XaRollbackOutcome.class, DEFAULT_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_INTERVAL_SECS, SECONDS);

        searchCompound = new CompoundOperationImpl(extractCacheStat(manager, "put"), SearchOutcome.class, DEFAULT_SEARCH_INTERVAL_SECS, SECONDS, executor, DEFAULT_HISTORY_SIZE, DEFAULT_SEARCH_INTERVAL_SECS, SECONDS);
    }
    
    private static <T extends Enum<T>> SourceStatistic<OperationObserver<T>> extractCacheStat(StatisticsManager manager, String name) {
        TreeNode node = manager.queryForSingleton(queryBuilder().children().ensureUnique()
                .children().filter(context(allOf(identifier(subclassOf(SourceStatistic.class)), attributes(hasAttribute("name", name))))).build());
        return (SourceStatistic<OperationObserver<T>>) node.getContext().attributes().get("this");
    }
    
    
    @Override
    public void setStatisticsTimeToDisable(long time, TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void setStatisticsEnabled(boolean enabled) {
        if (enabled) {
            getCompound.start();
            putCompound.start();
            removeCompound.start();
            evictedCompound.start();
            expiredCompound.start();
            heapGetCompound.start();
            offheapGetCompound.start();
            diskGetCompound.start();
            xaCommitCompound.start();
            xaRollbackCompound.start();
            searchCompound.start();
        } else {
            getCompound.stop();
            putCompound.stop();
            removeCompound.stop();
            evictedCompound.stop();
            expiredCompound.stop();
            heapGetCompound.stop();
            offheapGetCompound.stop();
            diskGetCompound.stop();
            xaCommitCompound.stop();
            xaRollbackCompound.stop();
            searchCompound.stop();
        }
    }

    @Override
    public CompoundOperation<GetOutcome> get() {
        return getCompound;
    }

    @Override
    public CompoundOperation<PutOutcome> put() {
        return putCompound;
    }

    @Override
    public CompoundOperation<RemoveOutcome> remove() {
        return removeCompound;
    }

    @Override
    public CompoundOperation<?> evicted() {
        return evictedCompound;
    }

    @Override
    public CompoundOperation<?> expired() {
        return expiredCompound;
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> heapGet() {
        return heapGetCompound;
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> offheapGet() {
        return offheapGetCompound;
    }

    @Override
    public CompoundOperation<StoreOperationOutcomes.GetOutcome> diskGet() {
        return diskGetCompound;
    }

    @Override
    public CompoundOperation<SearchOutcome> search() {
        return searchCompound;
    }

    @Override
    public CompoundOperation<XaCommitOutcome> xaCommit() {
        return xaCommitCompound;
    }

    @Override
    public CompoundOperation<XaRollbackOutcome> xaRollback() {
        return xaRollbackCompound;
    }
}
