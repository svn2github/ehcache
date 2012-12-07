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

import static org.terracotta.context.query.Matchers.allOf;
import static org.terracotta.context.query.Matchers.attributes;
import static org.terracotta.context.query.Matchers.context;
import static org.terracotta.context.query.Matchers.hasAttribute;
import static org.terracotta.context.query.Matchers.identifier;
import static org.terracotta.context.query.Matchers.subclassOf;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.CacheOperationOutcomes;
import net.sf.ehcache.Ehcache;

import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Query;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.ValueStatistic;

public class EhcacheStatisticsCoreDb {

    public final StatisticsManager statisticsManager = new StatisticsManager();
    private final boolean live = false;
    private final Ehcache cache;
    private final ConcurrentHashMap<String, OperationStatisticDescriptor> rawOperationStats = new ConcurrentHashMap<String, OperationStatisticDescriptor>();
    private final ConcurrentHashMap<String, ValueStatistic<Long>> operationValueStats = new ConcurrentHashMap<String, ValueStatistic<Long>>();
    private final OperationStatistic<CacheOperationOutcomes.GetOutcome> cacheGetOp;
    private final OperationStatistic<CacheOperationOutcomes.PutOutcome> cachePutOp;

    public EhcacheStatisticsCoreDb(Ehcache cache) {
        this.cache = cache;
        statisticsManager.root(cache);

        buildRawOperationStatistics();
        inferVisibleOperationStatistics();

        System.err.println("Operation Stats: ");
        for (Object o : rawOperationStats.values()) {
            System.err.println("\t" + o);
        }

        System.err.println("Raw Operation Value Stats: ");
        for (String vname : operationValueStats.keySet()) {
            System.err.println("\t" + vname);
        }

        cacheGetOp=probeForOpStatistic("/get");
        cachePutOp=probeForOpStatistic("/put");
    }

    private OperationStatistic probeForOpStatistic(String name) {
        OperationStatisticDescriptor descr = rawOperationStats.get(cache.getName() + name);
        if(descr==null) {
            return new OperationStatistic<CacheOperationOutcomes.GetOutcome>("void", Collections.<String>emptySet(), Collections.<String, Object>emptyMap(), CacheOperationOutcomes.GetOutcome.class);
        } else {
            return descr.getOpStatistic();
        }
    }

    private void inferVisibleOperationStatistics() {
        for (OperationStatisticDescriptor descr : rawOperationStats.values()) {
            for(Enum en:descr.getOutcome().getEnumConstants()) {
                ValueStatistic<Long> vstat = descr.getOpStatistic().statistic(en);
                String camel = en.name().toUpperCase().charAt(0) + en.name().toLowerCase().substring(1);
                operationValueStats.put(descr.getStringPath() + "/" + camel, vstat);
            }
        }
    }

    private void buildRawOperationStatistics() {
        rawOperationStats.clear();
        Query query = queryBuilder().descendants()
                .filter(context(allOf(identifier(subclassOf(Ehcache.class)), attributes(hasAttribute("name", cache.getName()))))).build();

        Collection<? extends TreeNode> res = statisticsManager.query(query);
        if (!res.isEmpty()) {

            query = queryBuilder().descendants().filter(context(allOf(identifier(subclassOf(OperationStatistic.class))))).build();

            Collection<? extends TreeNode> stats = statisticsManager.query(query);

            for (TreeNode tn : stats) {
                OperationStatisticDescriptor stat = new OperationStatisticDescriptor(cache, tn);
                rawOperationStats.put(stat.getStringPath(), stat);
            }
        }
    }
}