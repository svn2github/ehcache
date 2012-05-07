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
package net.sf.ehcache.config;

import net.sf.ehcache.statistics.sampled.CacheStatisticsSampler;
import net.sf.ehcache.util.counter.sampled.SampledCounterConfig;
import net.sf.ehcache.util.counter.sampled.SampledRateCounterConfig;

/**
 *  Configuration class of management REST services.
 *
 * @author Ludovic Orban
 */
public class ManagementRESTServiceConfiguration {

    /**
     * Default bind value.
     */
    public static final String DEFAULT_BIND = "0.0.0.0:9889";

    private volatile boolean enabled = false;
    private volatile String bind = DEFAULT_BIND;

    private volatile int sampleHistorySize = CacheStatisticsSampler.DEFAULT_HISTORY_SIZE;
    private volatile int sampleIntervalSeconds = CacheStatisticsSampler.DEFAULT_INTERVAL_SECS;
    private volatile int sampleSearchInterval = CacheStatisticsSampler.DEFAULT_SEARCH_INTERVAL_SEC;

    /**
     * Check if the REST services should be enabled or not.
     * @return true if REST services should be enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set that the REST services should be enabled or disabled.
     * @param enabled true if the REST services should be enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the host:port pair to which the REST server should be bound.
     * Format is: [IP address|host name]:[port number]
     * @return the host:port pair to which the REST server should be bound.
     */
    public String getBind() {
        return bind;
    }

    /**
     * Get the host part of the host:port pair to which the REST server should be bound.
     * @return the host part of the host:port pair to which the REST server should be bound.
     */
    public String getHost() {
        if (bind == null) {
            return null;
        }
        return bind.split("\\:")[0];
    }

    /**
     * Get the port part of the host:port pair to which the REST server should be bound.
     * @return the port part of the host:port pair to which the REST server should be bound.
     */
    public int getPort() {
        if (bind == null) {
            return -1;
        }
        String[] split = bind.split("\\:");
        if (split.length != 2) {
            throw new IllegalArgumentException("invalid bind format (should be IP:port)");
        }
        return Integer.parseInt(split[1]);
    }

    /**
     * Set the host:port pair to which the REST server should be bound.
     * @param bind host:port pair to which the REST server should be bound.
     */
    public void setBind(String bind) {
        this.bind = bind;
    }

    /**
     * Returns the sample history size to be applied to the {@link SampledCounterConfig} for sampled statistics
     *
     * @return the sample history size
     */
    public int getSampleHistorySize() {
        return sampleHistorySize;
    }

    /**
     * Sets the sample history size to be applied to the {@link SampledCounterConfig} for sampled statistics
     *
     * @param sampleHistorySize to set
     */
    public void setSampleHistorySize(final int sampleHistorySize) {
        this.sampleHistorySize = sampleHistorySize;
    }

    /**
     * Returns the sample interval in seconds to be applied to the {@link SampledCounterConfig} for sampled statistics
     *
     * @return the sample interval in seconds
     */
    public int getSampleIntervalSeconds() {
        return sampleIntervalSeconds;
    }

    /**
     * Sets the sample interval in seconds to be applied to the {@link SampledCounterConfig} for sampled statistics
     *
     * @param sampleIntervalSeconds to set
     */
    public void setSampleIntervalSeconds(final int sampleIntervalSeconds) {
        this.sampleIntervalSeconds = sampleIntervalSeconds;
    }

    /**
     * Returns the sample search interval in seconds to be applied to the {@link SampledRateCounterConfig} for sampled statistics
     *
     * @return the sample search interval in seconds
     */
    public int getSampleSearchIntervalSeconds() {
        return sampleSearchInterval;
    }

    /**
     * Sets the sample search interval in seconds to be applied to the {@link SampledCounterConfig} for sampled statistics
     *
     * @param sampleSearchInterval to set
     */
    public void setSampleSearchIntervalSeconds(final int sampleSearchInterval) {
        this.sampleSearchInterval = sampleSearchInterval;
    }

    /**
     * A factory method for {@link SampledCounterConfig} based on the global settings defined on this object
     *
     * @see #getSampleIntervalSeconds()
     * @see #getSampleHistorySize()
     *
     * @return a {@code SampledCounterConfig}
     */
    public SampledCounterConfig makeSampledCounterConfig() {
        return new SampledCounterConfig(getSampleIntervalSeconds(), getSampleHistorySize(), true, 0L);
    }

    /**
     * A factory method for {@link SampledCounterConfig} based on the global settings defined on this object
     *
     * @see #getSampleIntervalSeconds()
     * @see #getSampleHistorySize()
     *
     * @return a {@code SampledCounterConfig}
     */
    public SampledRateCounterConfig makeSampledGetRateCounterConfig() {
        return new SampledRateCounterConfig(getSampleIntervalSeconds(), getSampleHistorySize(), true);
    }

    /**
     * A factory method for {@link SampledCounterConfig} based on the global settings defined on this object
     *
     * @see #getSampleSearchIntervalSeconds()
     * @see #getSampleHistorySize()
     *
     * @return a {@code SampledCounterConfig}
     */
    public SampledRateCounterConfig makeSampledSearchRateCounterConfig() {
        return new SampledRateCounterConfig(getSampleSearchIntervalSeconds(), getSampleHistorySize(), true);
    }
}
