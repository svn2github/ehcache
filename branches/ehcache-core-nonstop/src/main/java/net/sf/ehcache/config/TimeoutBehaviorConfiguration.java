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
import net.sf.ehcache.constructs.nonstop.store.NonstopTimeoutBehaviorStoreType;

/**
 * Configuration element for configuring timeoutBehavior for nonstop
 *
 * @author Abhishek Sanoujam
 *
 */
public class TimeoutBehaviorConfiguration implements Cloneable {

    /**
     * Default value for timeout behavior
     */
    public final static String DEFAULT_VALUE = "exception";

    /**
     * Default value for properties.
     */
    public final static String DEFAULT_PROPERTIES = "";

    /**
     * Default value for property separator
     */
    public final static String DEFAULT_PROPERTY_SEPARATOR = ",";

    private String type = DEFAULT_VALUE;
    private String properties = DEFAULT_PROPERTIES;
    private String propertySeparator = DEFAULT_PROPERTY_SEPARATOR;

    /**
     * Returns the type of timeout behavior configured
     *
     * @return the configured type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of timeout behavior
     *
     * @param type
     */
    public void setType(String type) {
        if (!NonstopTimeoutBehaviorStoreType.isValidTimeoutBehaviorType(type)) {
            throw new CacheException("Invalid value for timeoutBehavior - '" + type + "'. Valid values are: "
                    + NonstopTimeoutBehaviorStoreType.getValidTimeoutBehaviors());
        }
        this.type = type;
    }

    /**
     * Set the type of timeout behavior
     *
     * @param type
     * @return this instance
     */
    public TimeoutBehaviorConfiguration type(String type) {
        this.setType(type);
        return this;
    }

    /**
     * Get the properties
     *
     * @return properties
     */
    public String getProperties() {
        return properties;
    }

    /**
     * Set the properties
     *
     * @param properties
     */
    public void setProperties(String properties) {
        this.properties = properties;
    }

    /**
     * Set the properties
     *
     * @param value
     * @return this instance
     */
    public TimeoutBehaviorConfiguration properties(String value) {
        this.setProperties(value);
        return this;
    }

    /**
     * Get the property separator
     *
     * @return the propery separator
     */
    public String getPropertySeparator() {
        return propertySeparator;
    }

    /**
     * Set the property separator
     *
     * @param propertySeparator
     */
    public void setPropertySeparator(String propertySeparator) {
        this.propertySeparator = propertySeparator;
    }

    /**
     * Set the property separator
     *
     * @param value
     * @return this instance
     */
    public TimeoutBehaviorConfiguration propertySeparator(String value) {
        this.setPropertySeparator(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
