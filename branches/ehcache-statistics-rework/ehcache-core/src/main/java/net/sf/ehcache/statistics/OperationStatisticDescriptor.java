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
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Ehcache;

import org.terracotta.context.TreeNode;
import org.terracotta.statistics.OperationStatistic;

/**
 * The Class OperationStatisticDescriptor.
 *
 * @author cschanck
 */
public class OperationStatisticDescriptor implements EhcacheStatisticDescriptor {

    /** The tn. */
    private final TreeNode tn;

    /** The short name. */
    private final String shortName;

    /** The outcome. */
    private final Class<? extends Enum> outcome;

    /** The op statistic. */
    private final OperationStatistic opStatistic;

    /** The cache. */
    private final Ehcache cache;

    /** The string paths. */
    private final String[] stringPaths;

    /** The tags. */
    private final Set<String> tags;

    /**
     * Instantiates a new operation statistic descriptor.
     *
     * @param cache the cache
     * @param tn the tn
     */
    public OperationStatisticDescriptor(Ehcache cache, TreeNode tn) {
        this.cache = cache;
        this.tn = tn;

        Map<String, Object> attrs = tn.getContext().attributes();
        this.shortName = (String) attrs.get("name");
        this.outcome = (Class<? extends Enum>) attrs.get("type");
        this.opStatistic = (OperationStatistic) attrs.get("this");
        this.tags = (Set<String>) attrs.get("tags");

        String[] paths = Constants.formStringPathsFromContext(tn);
        Arrays.sort(paths);
        this.stringPaths = paths;
    }

    /**
     * Gets the identifer.
     *
     * @return the identifer
     */
    public String getIdentifer() {
        return tn.getContext().identifier().getName();
    }

    /**
     * Gets the tree node.
     *
     * @return the tree node
     */
    public TreeNode getTreeNode() {
        return tn;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getTags()
     */
    @Override
    public Set<String> getTags() {
        return tags;
    };

    /**
     * Gets the outcome.
     *
     * @return the outcome
     */
    public Class<? extends Enum> getOutcome() {
        return outcome;
    }

    /**
     * Gets the op statistic.
     *
     * @return the op statistic
     */
    public OperationStatistic getOpStatistic() {
        return opStatistic;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getShortName()
     */
    @Override
    public String getShortName() {
        return shortName;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getStringPath()
     */
    @Override
    public String getStringPath() {
        return stringPaths[0];
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.EhcacheStatisticDescriptor#getCache()
     */
    @Override
    public Ehcache getCache() {
        return cache;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EhcacheOperationStatisticDescriptor [path=" + getStringPath() + ", shortName=" + shortName + ", outcome="
                + Arrays.asList(outcome.getEnumConstants()) + ", tags=" + tags + "]";
    }

}
