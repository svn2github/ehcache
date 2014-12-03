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

import net.sf.ehcache.statistics.StatisticsGateway;
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
    public static final String DEFAULT_BIND = "0.0.0.0:9888";

    /**
     * Synthetic bind value used when no server must be bound.
     */
    public static final String NO_BIND = "";

    /**
     * Synthetic security service location used when it should be auto-discovered.
     */
    public static final String AUTO_LOCATION = "";

    /**
     * Default timeout for the connection to the configured security service
     */
    public static final int DEFAULT_SECURITY_SVC_TIMEOUT = 5 * 1000;

    private volatile boolean enabled = false;
    private volatile String securityServiceLocation;
    private volatile boolean sslEnabled;
    private volatile boolean needClientAuth;
    private volatile int securityServiceTimeout = DEFAULT_SECURITY_SVC_TIMEOUT;
    private volatile String bind = DEFAULT_BIND;

    private volatile int sampleHistorySize = StatisticsGateway.DEFAULT_HISTORY_SIZE;
    private volatile int sampleIntervalSeconds = StatisticsGateway.DEFAULT_INTERVAL_SECS;
    private volatile int sampleSearchIntervalSeconds = StatisticsGateway.DEFAULT_SEARCH_INTERVAL_SECS;

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
     * Returns the security service location required for trusted identity assertion to the embedded REST management
     * service.  This feature is only available with an enterprise license.
     * <p/>
     * If this value is set, then this service will require secure dialog with the TMS or other 3rd party REST client
     * implementations. The service furnished by the enterprise version of the TMC is located is provided at /api/assertIdentity.
     *
     *
     * @return a string representing the URL of the security service.
     */
    public String getSecurityServiceLocation() {
        return securityServiceLocation;
    }

    /**
     * Sets the security service location required for trusted identity assertion to the embedded REST management
     * service.  This feature is only available with an enterprise license.
     * <p/>
     * If this value is set, then this service will require secure dialog with the TMS or other 3rd party REST client
     * implementations. The service furnished by the enterprise version of the TMC is located is provided at /api/assertIdentity.
     *
     * @param securityServiceURL a string representing the URL of the security service.
     */
    public void setSecurityServiceLocation(String securityServiceURL) {
        this.securityServiceLocation = securityServiceURL;
    }

    /**
     * Returns the connection/read timeout value for the security service in milliseconds.
     *
     * @return security service timeout
     */
    public int getSecurityServiceTimeout() {
        return securityServiceTimeout;
    }

    /**
     * Sets the connection/read timeout value for the security service in milliseconds.
     *
     * @param securityServiceTimeout milliseconds to timeout
     */
    public void setSecurityServiceTimeout(int securityServiceTimeout) {
        this.securityServiceTimeout = securityServiceTimeout;
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
            return -1;
        }
        return Integer.parseInt(split[1]);
    }

    /**
     * Indicates whether or not the embedded agent should enabled ssl.
     *
     * @return true if ssl should be enabled, false if not.
     */
    public boolean isSslEnabled() {
        return sslEnabled;
    }

    /**
     * Set ssl indicator for this embedded agent.
     *
     * @param sslEnabled boolean to indicate ssl status.
     */
    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    /**
     * Indicates whether or not the embedded agent should require ssl client certificate authorization.  This
     * configuration setting is only relevant if ssl is enabled.
     *
     * @see #isSslEnabled()
     *
     * @return true if ssl client certificate authorization should be required, false if not.
     */
    public boolean isNeedClientAuth() {
      return needClientAuth;
    }

    /**
     * Set ssl client certificate authorization required setting. This configuration setting is only relevant if ssl
     * is enabled.
     *
     * @see #setSslEnabled(boolean)
     *
     * @param needClientAuth
     */
    public void setNeedClientAuth(boolean needClientAuth) {
      this.needClientAuth = needClientAuth;
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
        return sampleSearchIntervalSeconds;
    }

    /**
     * Sets the sample search interval in seconds to be applied to the {@link SampledCounterConfig} for sampled statistics
     *
     * @param sampleSearchInterval to set
     */
    public void setSampleSearchIntervalSeconds(final int sampleSearchInterval) {
        this.sampleSearchIntervalSeconds = sampleSearchInterval;
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

    @Override
    public String toString() {
        return "ManagementRESTServiceConfiguration [enabled=" + enabled + ", securityServiceLocation=" + securityServiceLocation
                + ", sslEnabled=" + sslEnabled + ", needClientAuth=" + needClientAuth + ", securityServiceTimeout="
                + securityServiceTimeout + ", bind=" + bind + ", sampleHistorySize=" + sampleHistorySize + ", sampleIntervalSeconds="
                + sampleIntervalSeconds + ", sampleSearchIntervalSeconds=" + sampleSearchIntervalSeconds + "]";
    }
}
