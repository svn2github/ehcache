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

import net.sf.ehcache.CacheException;

/**
 * Configuration class of nonstop caches
 *
 * @author Abhishek Sanoujam
 * @author Eugene Kononov
 *
 */
public class NonstopConfiguration implements Cloneable {

    /**
     * System Property name for bulk operations multiply factor
     */
    public static final String BULK_OPS_TIMEOUT_MULTIPLY_FACTOR = "net.sf.ehcache.nonstop.bulkOpsTimeoutMultiplyFactor";

    /**
     * Default value of nonstop attribute
     */
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * Default value of immediateTimeout attribute
     */
    public static final boolean DEFAULT_IMMEDIATE_TIMEOUT = false;

    /**
     * Default value of timeoutMillis attribute
     */
    public static final int DEFAULT_TIMEOUT_MILLIS = 30000;

    /**
     * Default value of searchTimeoutMillis attribute
     */
    public static final int DEFAULT_SEARCH_TIMEOUT_MILLIS = 30000;

    /**
     * Default value of timeout multiplication factor for bulk operations like removeAll or size
     */
    public static final int DEFAULT_BULK_OP_TIMEOUT_FACTOR = Integer.getInteger(BULK_OPS_TIMEOUT_MULTIPLY_FACTOR, 10);

    /**
     * Default value of timeoutBehavior attribute
     */
    public static final TimeoutBehaviorConfiguration DEFAULT_TIMEOUT_BEHAVIOR = new TimeoutBehaviorConfiguration();

    private volatile boolean enabled = DEFAULT_ENABLED;
    private volatile boolean immediateTimeout = DEFAULT_IMMEDIATE_TIMEOUT;
    private volatile long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
    private volatile long searchTimeoutMillis = DEFAULT_SEARCH_TIMEOUT_MILLIS;
    private volatile int bulkOpsTimeoutMultiplyFactor = DEFAULT_BULK_OP_TIMEOUT_FACTOR;
    private TimeoutBehaviorConfiguration timeoutBehavior = DEFAULT_TIMEOUT_BEHAVIOR;
    private volatile boolean configFrozen;

    /**
     * Freeze the config. Once frozen, 'enabled' can't be changed
     */
    public void freezeConfig() {
        configFrozen = true;
    }

    /**
     * Returns true if nonstop is enabled in config
     *
     * @return true if nonstop is enabled in config
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the value of nonstop is enabled or not
     *
     * @param enabled the new value
     */
    public void setEnabled(boolean enabled) {
        if (configFrozen) {
            throw new CacheException("NonstopConfiguration cannot be enabled or disabled after Cache has been initialized.");
        }
        this.enabled = enabled;
    }

    /**
     * Set the value of nonstop
     *
     * @param nonstop
     * @return this configuration instance
     */
    public NonstopConfiguration enabled(boolean nonstop) {
        this.setEnabled(nonstop);
        return this;
    }

    /**
     * Returns true if immediateTimeout is set to true
     *
     * @return true if immediateTimeout is set to true
     */
    public boolean isImmediateTimeout() {
        return immediateTimeout;
    }

    /**
     * Set the value of immediateTimeout
     *
     * @param immediateTimeout the new value
     */
    public void setImmediateTimeout(boolean immediateTimeout) {
        this.immediateTimeout = immediateTimeout;
    }

    /**
     * Set the value of immediateTimeout
     *
     * @param immediateTimeout
     * @return this configuration instance
     */
    public NonstopConfiguration immediateTimeout(boolean immediateTimeout) {
        this.setImmediateTimeout(immediateTimeout);
        return this;
    }

    /**
     * Returns the value of timeout in milliseconds
     *
     * @return the value of timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Set the value of timeoutMillis
     *
     * @param timeoutMillis the new value
     */
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Returns the value of the search timeout in milliseconds
     *
     * @return the value of the search timeout in milliseconds
     */
    public long getSearchTimeoutMillis() {
        return searchTimeoutMillis;
    }

    /**
     * Set the value of the search timeout
     *
     * @param searchTimeoutMillis the new value
     */
    public void setSearchTimeoutMillis(long searchTimeoutMillis) {
        this.searchTimeoutMillis = searchTimeoutMillis;
    }

    /**
     * returns the time out multiplication factor for bulk cache operations
     *
     * @return the value of factor
     */
    public int getBulkOpsTimeoutMultiplyFactor() {
        return bulkOpsTimeoutMultiplyFactor;
    }

    /**
     * Sets the value of the multiplication factor for bulk cache operations
     *
     * @param bulkOpsTimeoutMultiplyFactor the new value
     */
    public void setBulkOpsTimeoutMultiplyFactor(int bulkOpsTimeoutMultiplyFactor) {
        this.bulkOpsTimeoutMultiplyFactor = bulkOpsTimeoutMultiplyFactor;
    }

    /**
     * Set the value of timeoutMillis
     *
     * @param timeoutMillis the new value
     * @return this configuration instance
     */
    public NonstopConfiguration timeoutMillis(long timeoutMillis) {
        this.setTimeoutMillis(timeoutMillis);
        return this;
    }

    /**
     * Set the value of the search timeout
     *
     * @param searchTimeoutMillis the new value of the search timeout in milliseconds
     * @return this configuration instance
     */
    public NonstopConfiguration searchTimeoutMillis(long searchTimeoutMillis) {
        this.setSearchTimeoutMillis(searchTimeoutMillis);
        return this;
    }


    /**
     * Returns value of timeoutBehavior configured
     *
     * @return value of timeoutBehavior configured
     */
    public TimeoutBehaviorConfiguration getTimeoutBehavior() {
        return timeoutBehavior;
    }

    /**
     * Set the value of timeoutBehavior
     *
     * @param timeoutBehavior
     */
    public void addTimeoutBehavior(TimeoutBehaviorConfiguration timeoutBehavior) {
        this.timeoutBehavior = timeoutBehavior;
    }

    /**
     * Set the value of timeoutBehavior
     *
     * @param timeoutBehavior
     * @return this configuration instance
     */
    public NonstopConfiguration timeoutBehavior(TimeoutBehaviorConfiguration timeoutBehavior) {
        this.addTimeoutBehavior(timeoutBehavior);
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + bulkOpsTimeoutMultiplyFactor;
        result = prime * result + (configFrozen ? 1231 : 1237);
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + (immediateTimeout ? 1231 : 1237);
        result = prime * result + ((timeoutBehavior == null) ? 0 : timeoutBehavior.hashCode());
        result = prime * result + (int) (timeoutMillis ^ (timeoutMillis >>> 32));
        result = prime * result + (int) (searchTimeoutMillis ^ (searchTimeoutMillis >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NonstopConfiguration other = (NonstopConfiguration) obj;
        if (bulkOpsTimeoutMultiplyFactor != other.bulkOpsTimeoutMultiplyFactor ||
            configFrozen != other.configFrozen ||
            enabled != other.enabled ||
            immediateTimeout != other.immediateTimeout ||
            searchTimeoutMillis != other.searchTimeoutMillis ||
            timeoutMillis != other.timeoutMillis) {
            return false;
        }
        if (timeoutBehavior == null) {
            if (other.timeoutBehavior != null) {
                return false;
            }
        } else if (!timeoutBehavior.equals(other.timeoutBehavior)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NonstopConfiguration clone() throws CloneNotSupportedException {
        try {
            NonstopConfiguration clone = (NonstopConfiguration) super.clone();
            clone.addTimeoutBehavior((TimeoutBehaviorConfiguration) timeoutBehavior.clone());
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new CacheException(e);
        }
    }
}
