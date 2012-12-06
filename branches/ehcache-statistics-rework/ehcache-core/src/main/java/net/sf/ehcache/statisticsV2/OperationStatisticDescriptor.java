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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Ehcache;

import org.terracotta.context.TreeNode;
import org.terracotta.statistics.OperationStatistic;

public class OperationStatisticDescriptor implements EhcacheStatisticDescriptor {

    private final Map<String, Object> attrs;
    private final TreeNode tn;
    private final Map<String, Object> properties;
    private final String shortName;
    private final Class<? extends Enum> outcome;
    private final OperationStatistic opStatistic;
    private final List<? extends TreeNode> path;
    private final Ehcache cache;
    private final String[] stringPaths;
    private final Set<String> tags;

    public OperationStatisticDescriptor(Ehcache cache, TreeNode tn) {
        this.cache = cache;
        this.tn = tn;
        this.attrs = tn.getContext().attributes();
        this.properties = (Map<String, Object>) attrs.get(Constants.PROPERTIES_PROP);
        this.shortName = (String) properties.get(Constants.NAME_PROP);
        this.outcome = (Class<? extends Enum>) attrs.get("type");
        this.opStatistic = (OperationStatistic) attrs.get("this");
        this.path = tn.getPath();
        if(properties.containsKey(EhcacheStatisticsPropertyMap.TAGS_PROP)) {
            tags=Collections.unmodifiableSet((Set<String>) properties.get(EhcacheStatisticsPropertyMap.TAGS_PROP));
        } else {
            tags=Collections.EMPTY_SET;
        }
        // must always be one, and one is enough.

        String[] paths = Constants.formStringPathsFromContext(tn);
        Arrays.sort(paths);
        this.stringPaths=paths;

    }

    public String getIdentifer() {
        return tn.getContext().identifier().getName();
    }

    public TreeNode getTreeNode() {
        return tn;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getTags()
     */
    @Override
    public Collection<String> getTags() {
        return tags;
    };

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getRecordingCost()
     */
    @Override
    public Constants.RecordingCost getRecordingCost() {
        return Constants.RecordingCost.UNKNOWN;
    };

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getRetrievalCost()
     */
    @Override
    public Constants.RetrievalCost getRetrievalCost() {
        return Constants.RetrievalCost.UNKNOWN;
    }

    public Class<? extends Enum> getOutcome() {
        return outcome;
    }

    public OperationStatistic getOpStatistic() {
        return opStatistic;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getShortName()
     */
    @Override
    public String getShortName() {
        return shortName;
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getStringPath()
     */
    @Override
    public String getStringPath() {
        return stringPaths[0];
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getCache()
     */
    @Override
    public Ehcache getCache() {
        return cache;
    }

    @Override
    public String toString() {
        return "EhcacheOperationStatisticDescriptor [path=" + getStringPath() + ", shortName=" + shortName + ", outcome=" + Arrays.asList(outcome.getEnumConstants())
                + ", properties=" + properties + "]";
    }

}
