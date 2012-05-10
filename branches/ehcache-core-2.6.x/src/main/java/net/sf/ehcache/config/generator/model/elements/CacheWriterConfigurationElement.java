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

import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration.CacheWriterFactoryConfiguration;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

/**
 * Element representing the {@link CacheWriterConfiguration}
 *
 * @author Abhishek Sanoujam
 *
 */
public class CacheWriterConfigurationElement extends SimpleNodeElement {

    private final CacheWriterConfiguration cacheWriterConfiguration;

    /**
     * Constructor accepting the parent and the {@link CacheWriterConfiguration}
     *
     * @param parent
     * @param cacheWriterConfiguration
     */
    public CacheWriterConfigurationElement(NodeElement parent, CacheWriterConfiguration cacheWriterConfiguration) {
        super(parent, "cacheWriter");
        this.cacheWriterConfiguration = cacheWriterConfiguration;
        init();
    }

    private void init() {
        if (cacheWriterConfiguration == null) {
            return;
        }
        addAttribute(new SimpleNodeAttribute("minWriteDelay", cacheWriterConfiguration.getMinWriteDelay()).optional(true).defaultValue(
                CacheWriterConfiguration.DEFAULT_MIN_WRITE_DELAY));
        addAttribute(new SimpleNodeAttribute("writeMode", cacheWriterConfiguration.getWriteMode()).optional(true).defaultValue(
                CacheWriterConfiguration.DEFAULT_WRITE_MODE));
        addAttribute(new SimpleNodeAttribute("writeBatchSize", cacheWriterConfiguration.getWriteBatchSize()).optional(true).defaultValue(
                CacheWriterConfiguration.DEFAULT_WRITE_BATCH_SIZE));
        addAttribute(new SimpleNodeAttribute("maxWriteDelay", cacheWriterConfiguration.getMaxWriteDelay()).optional(true).defaultValue(
                CacheWriterConfiguration.DEFAULT_MAX_WRITE_DELAY));
        addAttribute(new SimpleNodeAttribute("retryAttempts", cacheWriterConfiguration.getRetryAttempts()).optional(true).defaultValue(
                CacheWriterConfiguration.DEFAULT_RETRY_ATTEMPTS));
        addAttribute(new SimpleNodeAttribute("rateLimitPerSecond", cacheWriterConfiguration.getRateLimitPerSecond()).optional(true)
                .defaultValue(CacheWriterConfiguration.DEFAULT_RATE_LIMIT_PER_SECOND));
        addAttribute(new SimpleNodeAttribute("writeBatching", cacheWriterConfiguration.getWriteBatching()).optional(true).defaultValue(
                CacheWriterConfiguration.DEFAULT_WRITE_BATCHING));
        addAttribute(new SimpleNodeAttribute("writeCoalescing", cacheWriterConfiguration.getWriteCoalescing()).optional(true).defaultValue(
                CacheWriterConfiguration.DEFAULT_WRITE_COALESCING));
        addAttribute(new SimpleNodeAttribute("notifyListenersOnException", cacheWriterConfiguration.getNotifyListenersOnException())
                .optional(true).defaultValue(CacheWriterConfiguration.DEFAULT_NOTIFY_LISTENERS_ON_EXCEPTION));
        addAttribute(new SimpleNodeAttribute("retryAttemptDelaySeconds", cacheWriterConfiguration.getRetryAttemptDelaySeconds()).optional(
                true).defaultValue(CacheWriterConfiguration.DEFAULT_RETRY_ATTEMPT_DELAY_SECONDS));
        addAttribute(new SimpleNodeAttribute("writeBehindConcurrency", cacheWriterConfiguration.getWriteBehindConcurrency()).optional(
                true).defaultValue(CacheWriterConfiguration.DEFAULT_WRITE_BEHIND_CONCURRENCY));
        addAttribute(new SimpleNodeAttribute("writeBehindMaxQueueSize", cacheWriterConfiguration.getWriteBehindMaxQueueSize()).optional(
                true).defaultValue(CacheWriterConfiguration.DEFAULT_WRITE_BEHIND_MAX_QUEUE_SIZE));

        CacheWriterFactoryConfiguration cacheWriterFactoryConfiguration = cacheWriterConfiguration.getCacheWriterFactoryConfiguration();
        if (cacheWriterFactoryConfiguration != null) {
            addChildElement(new FactoryConfigurationElement(this, "cacheWriterFactory", cacheWriterFactoryConfiguration));
        }
    }

}
