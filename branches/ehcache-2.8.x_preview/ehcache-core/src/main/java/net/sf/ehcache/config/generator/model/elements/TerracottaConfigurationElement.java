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

package net.sf.ehcache.config.generator.model.elements;

import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * {@link NodeElement} representing the {@link TerracottaConfiguration}
 *
 * @author Abhishek Sanoujam
 *
 */
public class TerracottaConfigurationElement extends SimpleNodeElement {

    private final TerracottaConfiguration tcConfiguration;

    /**
     * Constructor accepting the parent and the {@link TerracottaConfiguration}
     *
     * @param parent
     * @param tcConfiguration
     */
    public TerracottaConfigurationElement(NodeElement parent, TerracottaConfiguration tcConfiguration) {
        super(parent, "terracotta");
        this.tcConfiguration = tcConfiguration;
        init();
    }

    private void init() {
        if (tcConfiguration == null) {
            return;
        }
        if (!TerracottaConfiguration.DEFAULT_NON_STOP_CONFIGURATION.equals(tcConfiguration.getNonstopConfiguration())) {
            this.addChildElement(new NonstopConfigurationElement(this, tcConfiguration.getNonstopConfiguration()));
        }
        addAttribute(new SimpleNodeAttribute("clustered", tcConfiguration.isClustered()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_CLUSTERED));
        addAttribute(new SimpleNodeAttribute("consistency", tcConfiguration.getConsistency().name()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_CONSISTENCY_TYPE.name()));
        addAttribute(new SimpleNodeAttribute("synchronousWrites", tcConfiguration.isSynchronousWrites()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_SYNCHRONOUS_WRITES));
        addAttribute(new SimpleNodeAttribute("copyOnRead", tcConfiguration.isCopyOnRead()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_COPY_ON_READ));
        addAttribute(new SimpleNodeAttribute("localKeyCache", tcConfiguration.getLocalKeyCache()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_LOCAL_KEY_CACHE));
        addAttribute(new SimpleNodeAttribute("localKeyCacheSize", tcConfiguration.getLocalKeyCacheSize()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_LOCAL_KEY_CACHE_SIZE));
        addAttribute(new SimpleNodeAttribute("orphanEviction", tcConfiguration.getOrphanEviction()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_ORPHAN_EVICTION));
        addAttribute(new SimpleNodeAttribute("orphanEvictionPeriod", tcConfiguration.getOrphanEvictionPeriod()).optional(true)
                .defaultValue(TerracottaConfiguration.DEFAULT_ORPHAN_EVICTION_PERIOD));
        addAttribute(new SimpleNodeAttribute("coherentReads", tcConfiguration.getCoherentReads()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_COHERENT_READS));
        addAttribute(new SimpleNodeAttribute("concurrency", tcConfiguration.getConcurrency()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_CONCURRENCY));
        addAttribute(new SimpleNodeAttribute("localCacheEnabled", tcConfiguration.isLocalCacheEnabled()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_LOCAL_CACHE_ENABLED));
        addAttribute(new SimpleNodeAttribute("compressionEnabled", tcConfiguration.isCompressionEnabled()).optional(true).defaultValue(
                TerracottaConfiguration.DEFAULT_COMPRESSION_ENABLED));

    }

}
