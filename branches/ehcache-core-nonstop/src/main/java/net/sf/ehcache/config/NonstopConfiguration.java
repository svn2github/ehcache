/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehaviorType;

/**
 * Configuration class of nonstop caches
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopConfiguration implements Cloneable {

    /**
     * Default value of nonstop attribute
     */
    public static final boolean DEFAULT_NONSTOP = true;

    /**
     * Default value of immediateTimeout attribute
     */
    public static final boolean DEFAULT_IMMEDIATE_TIMEOUT = true;

    /**
     * Default value of timeoutMillis attribute
     */
    public static final int DEFAULT_TIMEOUT_MILLIS = 5000;

    /**
     * Default value of timeoutBehavior attribute
     */
    public static final String DEFAULT_NONSTOP_TIMEOUT_BEHAVIOR = "exception";

    private boolean nonstop = DEFAULT_NONSTOP;
    private boolean immediateTimeout = DEFAULT_IMMEDIATE_TIMEOUT;
    private int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
    private String timeoutBehavior = DEFAULT_NONSTOP_TIMEOUT_BEHAVIOR;

    /**
     * Returns true if nonstop is enabled in config
     *
     * @return true if nonstop is enabled in config
     */
    public boolean isNonstop() {
        return nonstop;
    }

    /**
     * Set the value of nonstop is enabled or not
     *
     * @param nonstop the new value
     */
    public void setNonstop(boolean nonstop) {
        this.nonstop = nonstop;
    }

    /**
     * Set the value of nonstop
     *
     * @param nonstop
     * @return this configuration instance
     */
    public NonstopConfiguration nonstop(boolean nonstop) {
        this.setNonstop(nonstop);
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
    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Set the value of timeoutMillis
     *
     * @param timeoutMillis the new value
     */
    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Set the value of timeoutMillis
     *
     * @param timeoutMillis the new value
     * @return this configuration instance
     */
    public NonstopConfiguration timeoutMillis(int timeoutMillis) {
        this.setTimeoutMillis(timeoutMillis);
        return this;
    }

    /**
     * Returns value of timeoutBehavior configured
     *
     * @return value of timeoutBehavior configured
     */
    public String getTimeoutBehavior() {
        return timeoutBehavior;
    }

    /**
     * Set the value of timeoutBehavior
     *
     * @param timeoutBehavior
     */
    public void setTimeoutBehavior(String timeoutBehavior) {
        if (!NonStopCacheBehaviorType.isValidTimeoutValue(timeoutBehavior)) {
            throw new CacheException("Invalid value for timeoutBehavior - '" + timeoutBehavior + "'. Valid values are: "
                    + NonStopCacheBehaviorType.getValidTimeoutBehaviors());
        }
        this.timeoutBehavior = timeoutBehavior;
    }

    /**
     * Set the value of timeoutBehavior
     *
     * @param timeoutBehavior
     * @return this configuration instance
     */
    public NonstopConfiguration timeoutBehavior(String timeoutBehavior) {
        this.setTimeoutBehavior(timeoutBehavior);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NonstopConfiguration clone() throws CloneNotSupportedException {
        try {
            return (NonstopConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new CacheException(e);
        }
    }

}
