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
package net.sf.ehcache.management.sampled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.query.QueryManagerBuilder;
import net.sf.ehcache.statistics.FlatStatistics;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.StoreQuery.Ordering;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

/**
 * An implementation of {@link CacheManagerSampler}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public class CacheManagerSamplerImpl implements CacheManagerSampler {

    private static final int MAX_QUERY_RESULT_LIMIT = 1000;

    private final CacheManager cacheManager;


    /**
     * Constructor taking the backing {@link CacheManager}
     *
     * @param cacheManager to wrap
     */
    public CacheManagerSamplerImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * {@inheritDoc}
     */
    public void clearAll() {
        cacheManager.clearAll();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getCacheNames() throws IllegalStateException {
        return cacheManager.getCacheNames();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return cacheManager.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, long[]> getCacheMetrics() {
        Map<String, long[]> result = new HashMap<String, long[]>();
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                FlatStatistics stats = cache.getStatistics();
                result.put(cacheName, new long[] {stats.cacheHitOperation().rate().value().longValue(),
                    stats.cacheMissExpiredOperation().rate().value().longValue(),
                    stats.cacheMissNotFoundOperation().rate().value().longValue(),
                    stats.cachePutOperation().rate().value().longValue()});
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                long val = cache.getStatistics().cacheHitOperation().rate().value().longValue();
                result += val;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheInMemoryHitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                long val = cache.getStatistics().localHeapHitOperation().rate().value().longValue();
                result += val;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOffHeapHitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                long val = cache.getStatistics().localOffHeapHitOperation().rate().value().longValue();
                result += val;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOnDiskHitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                long val = cache.getStatistics().localDiskHitOperation().rate().value().longValue();
                result += val;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                long val = cache.getStatistics().cacheMissOperation().rate().value().longValue();
                result += val;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheInMemoryMissRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                long val = cache.getStatistics().localHeapMissOperation().rate().value().longValue();
                result += val;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOffHeapMissRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().localOffHeapMissOperation().rate().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOnDiskMissRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().localDiskMissOperation().rate().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCachePutRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().cachePutOperation().rate().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheUpdateRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().cachePutReplacedOperation().rate().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheRemoveRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().cacheRemoveOperation().rate().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheEvictionRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().cacheEvictionOperation().rate().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheExpirationRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().cacheExpiredOperation().rate().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public float getCacheAverageGetTime() {
        float result = 0;
        int instances = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().cacheSearchOperation().latency().average().value().longValue();
                instances++;
            }
        }
        return instances > 0 ? result / instances : 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheSearchRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().cacheSearchOperation().rate().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheAverageSearchTime() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getStatistics().cacheSearchOperation().latency().average().value().longValue();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getHasWriteBehindWriter() {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                if (cache.getWriterManager() instanceof WriteBehindManager &&
                    cache.getRegisteredCacheWriter() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public long getWriterQueueLength() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += Math.max(cache.getStatistics().getWriterQueueLength(), 0);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int getWriterMaxQueueSize() {
        int result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                CacheWriterConfiguration writerConfig = cache.getCacheConfiguration().getCacheWriterConfiguration();
                result += (writerConfig.getWriteBehindMaxQueueSize() * writerConfig.getWriteBehindConcurrency());
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalDisk() {
        return cacheManager.getConfiguration().getMaxBytesLocalDisk();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalDiskAsString() {
        return cacheManager.getConfiguration().getMaxBytesLocalDiskAsString();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalDisk(long maxBytes) {
        try {
            cacheManager.getConfiguration().setMaxBytesLocalDisk(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalDiskAsString(String maxBytes) {
        try {
            cacheManager.getConfiguration().setMaxBytesLocalDisk(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalHeap() {
        return cacheManager.getConfiguration().getMaxBytesLocalHeap();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalHeapAsString() {
        return cacheManager.getConfiguration().getMaxBytesLocalHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalHeap(long maxBytes) {
        try {
            cacheManager.getConfiguration().setMaxBytesLocalHeap(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalHeapAsString(String maxBytes) {
        try {
            cacheManager.getConfiguration().setMaxBytesLocalHeap(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalOffHeap() {
        return cacheManager.getConfiguration().getMaxBytesLocalOffHeap();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalOffHeapAsString() {
        return cacheManager.getConfiguration().getMaxBytesLocalOffHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return cacheManager.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getClusterUUID() {
        return cacheManager.getClusterUUID();
    }

    /**
     * {@inheritDoc}
     */
    public String generateActiveConfigDeclaration() {
        return this.cacheManager.getActiveConfigurationText();
    }

    /**
     * {@inheritDoc}
     */
    public String generateActiveConfigDeclaration(String cacheName) {
        return this.cacheManager.getActiveConfigurationText(cacheName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean getTransactional() {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null && cache.getCacheConfiguration().getTransactionalMode().isTransactional()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getSearchable() {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null && cache.getCacheConfiguration().getSearchable() != null) {
                return true;
            }
        }
        return false;
    }

    /*
     * Ensure limit is not greater than 1000 to avoid OOME's.
     *
     * Have to manually clone a new query due to weird lifecycle of querys wherein they can be frozen
     * yet you can't invoke getters if it's NOT frozen.
     */
    private Query limitResults(Query q) {
        StoreQuery sq = (StoreQuery)q;
        int maxResults = sq.maxResults();

        if (maxResults == -1 || maxResults > MAX_QUERY_RESULT_LIMIT) {
            Query newQuery = sq.getCache().createQuery().maxResults(MAX_QUERY_RESULT_LIMIT);

            if (sq.requestsKeys()) {
                newQuery.includeKeys();
            }

            if (sq.requestsValues()) {
                newQuery.includeValues();
            }

            Set<Attribute<?>> attrs = sq.requestedAttributes();
            if (attrs != null) {
                newQuery.includeAttribute(new ArrayList<Attribute<?>>(attrs).toArray(new Attribute<?>[0]));
            }

            Criteria criteria = sq.getCriteria();
            if (criteria != null) {
                newQuery.addCriteria(criteria);
            }

            Set<Attribute<?>> groupByAttrs = sq.groupByAttributes();
            if (groupByAttrs != null) {
                newQuery.addGroupBy(new ArrayList<Attribute<?>>(groupByAttrs).toArray(new Attribute<?>[0]));
            }

            List<Ordering> orderings = sq.getOrdering();
            if (orderings != null) {
                for (Ordering ordering : orderings) {
                    newQuery.addOrderBy(ordering.getAttribute(), ordering.getDirection());
                }
            }

            List<Aggregator> aggregators = sq.getAggregators();
            if (aggregators != null) {
                newQuery.includeAggregator(aggregators.toArray(new Aggregator[0]));
            }

            newQuery.targets(sq.getTargets());

            q = newQuery.end();
        }

        return q;
    }

    /**
     * If the value is a primitive, return it, else return string form.
     */
    private Object primitiveOrString(Object value) {
        if (value != null) {
            if (!value.getClass().isPrimitive()) {
                value = value.toString();
            }
        }
        return value;
    }

    @Override
    public Object[][] executeQuery(String queryString) throws SearchException {
        QueryManagerBuilder qmb = QueryManagerBuilder.newQueryManagerBuilder();
        return executeQuery(queryString, qmb);
    }

    /**
    * Execute a BMSQL query against the CacheManager and return result grid.
    *
    * @param queryString
    * @param qmb the QueryManagerBuilder to use for this query
    * @return
    * @throws SearchException
    */
    Object[][] executeQuery(String queryString, QueryManagerBuilder qmb) throws SearchException {
      boolean searchable = false;
      for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null && cache.getCacheConfiguration().getSearchable() != null) {
                qmb.addCache(cache);
                searchable = true;
            }
        }

        if (!searchable) {
            throw new SearchException("There are no searchable caches");
        }

        Query q = limitResults(qmb.build().createQuery(queryString).end());
        StoreQuery sq = (StoreQuery)q;

        Set<Attribute<?>> attrs = new HashSet<Attribute<?>>(sq.requestedAttributes());

        if (sq.requestsKeys()) {
            attrs.add(Query.KEY);
        }
        if (sq.requestsValues()) {
            attrs.add(Query.VALUE);
        }

        Map<String, Attribute<?>> attrMap = new HashMap<String, Attribute<?>>();
        for (Attribute<?> attr : attrs) {
            String attrName = attr.getAttributeName();
            attrMap.put(attrName, attr);
        }

        String[] selectTargets = sq.getTargets();
        Results results = q.execute();
        List<Result> all = results.all();
        List<Object[]> result = new ArrayList<Object[]>(results.size());
        List<Object> row = new ArrayList<Object>();
        Map<String, String> typeMap = new HashMap<String, String>();

        for (Result r: all) {
            int aggregateIndex = 0;

            for (String target : selectTargets) {
                Attribute<?> attr = attrMap.get(target);
                Object value;

                if (attr != null) {
                    if (attr == Query.KEY) {
                        value = primitiveOrString(r.getKey());
                    } else if (attr == Query.VALUE) {
                        value = primitiveOrString(r.getValue());
                    } else {
                        value = r.getAttribute(attr);
                        if (value != null && value.getClass().isEnum()) {
                            value = ((Enum)value).name();
                        }
                    }
                } else {
                    value = r.getAggregatorResults().get(aggregateIndex++);
                }
                row.add(value);

                if (typeMap.get(target) == null && value != null) {
                    typeMap.put(target, value.getClass().getSimpleName());
                }
            }

            if (row.size() > 0) {
                result.add(row.toArray(new Object[0]));
                row.clear();
            }
        }

        row.clear();

        Map<String, SearchAttribute> sas = sq.getCache().getCacheConfiguration().getSearchAttributes();
        for (String target : selectTargets) {
            String typeName = typeMap.get(target);
            if (typeName == null) {
                SearchAttribute sa = sas.get(target);
                if (sa != null) {
                    typeName = sa.getTypeName();
                }
            }
            row.add(target + ":" + (typeName != null ? typeName : ""));
        }
        result.add(0, row.toArray(new String[0]));

        results.discard();

        return result.toArray(new Object[all.size()][]);
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionCommittedCount() {
        return this.cacheManager.getTransactionController().getTransactionCommittedCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionCommitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                long val = cache.getStatistics().xaCommitSuccessOperation().rate().value().longValue();
                result += val;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionRolledBackCount() {
        return this.cacheManager.getTransactionController().getTransactionRolledBackCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionRollbackRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
               long val = cache.getStatistics().xaRollbackOperation().rate().value().longValue();
               result += val;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionTimedOutCount() {
        return this.cacheManager.getTransactionController().getTransactionTimedOutCount();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled() throws CacheException {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null && cache.isDisabled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setEnabled(boolean enabled) {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                cache.setDisabled(!enabled);
            }
        }
    }
}