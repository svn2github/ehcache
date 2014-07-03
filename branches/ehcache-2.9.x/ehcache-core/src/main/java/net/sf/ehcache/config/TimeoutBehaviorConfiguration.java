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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.ehcache.CacheException;

/**
 * Configuration element for configuring timeoutBehavior for nonstop
 *
 * @author Abhishek Sanoujam
 *
 */
public class TimeoutBehaviorConfiguration implements Cloneable {
    /**
     * Type name for {@link TimeoutBehaviorType#EXCEPTION}
     */
    public static final String EXCEPTION_TYPE_NAME = "exception";
    /**
     * Type name for {@link TimeoutBehaviorType#LOCAL_READS}
     */
    public static final String LOCAL_READS_TYPE_NAME = "localReads";
    /**
     * Type name for {@link TimeoutBehaviorType#LOCAL_READS_AND_EXCEPTION_ON_WRITES}
     */
    public static final String LOCAL_READS_AND_EXCEPTION_ON_WRITES_TYPE_NAME = "localReadsAndExceptionOnWrite";
    /**
     * Type name for {@link TimeoutBehaviorType#NOOP}
     */
    public static final String NOOP_TYPE_NAME = "noop";
    /**
     * Type name for {@link TimeoutBehaviorType#CUSTOM}
     */
    public static final String CUSTOM_TYPE_NAME = "custom";

    /**
     * Property name used to configure the class name of the factory class used by {@link TimeoutBehaviorType#CUSTOM}
     */
    public static final String CUSTOM_TYPE_FACTORY_PROPERTY_NAME = "customFactoryClassName";

    /**
     * Enum encapsulating type of TimeoutBehavior
     *
     * @author Abhishek Sanoujam
     *
     */
    public static enum TimeoutBehaviorType {
        /**
         * Timeout behavior type that throws exception on timeout
         */
        EXCEPTION() {

            /**
             * {@inheritDoc}
             */
            @Override
            public String getTypeName() {
                return EXCEPTION_TYPE_NAME;
            }

        },
        /**
         * Timeout behavior type that returns null and does nothing on timeout
         */
        NOOP() {

            /**
             * {@inheritDoc}
             */
            @Override
            public String getTypeName() {
                return NOOP_TYPE_NAME;
            }


        },
        /**
         * Timeout behavior type that returns local values present in the VM or otherwise null on timeout
         */
        LOCAL_READS() {

            /**
             * {@inheritDoc}
             */
            @Override
            public String getTypeName() {
                return LOCAL_READS_TYPE_NAME;
            }

        },
        /**
         * Timeout behavior type that returns local values present in the VM or otherwise null on timeout for read operations.
         * For write operations, it throws an exception on timeout.
         */
        LOCAL_READS_AND_EXCEPTION_ON_WRITES() {

            /**
             * {@inheritDoc}
             */
            @Override
            public String getTypeName() {
                return LOCAL_READS_AND_EXCEPTION_ON_WRITES_TYPE_NAME;
            }

        },
        /**
         * Timeout behavior type that uses a custom factory to create the actual timeout behavior on timeout. The custom factory has to be
         * configured using properties otherwise an exception will be thrown. There must be a property named
         * {@link TimeoutBehaviorConfiguration#CUSTOM_TYPE_FACTORY_PROPERTY_NAME} whose value is the
         * fully qualified name of a class that implements {@link NonstopTimeoutBehaviorFactory} having no-args constructor.
         */
        CUSTOM() {

            /**
             * {@inheritDoc}
             */
            @Override
            public String getTypeName() {
                return CUSTOM_TYPE_NAME;
            }

        };

        /**
         * Returns a String signifying this type
         *
         * @return the string name for this type
         */
        public abstract String getTypeName();

        /**
         * Get the {@link NonstopTimeoutBehaviorFactory} for this type
         *
         * @param properties The configured properties
         * @return the factory to create timeout behaviors for this type
         */

        private static final Map<String, TimeoutBehaviorType> TYPE_MAP;
        static {
            Map<String, TimeoutBehaviorType> validTypes = new HashMap<String, TimeoutBehaviorType>();
            for (TimeoutBehaviorType timeoutBehaviorType : TimeoutBehaviorType.values()) {
                validTypes.put(timeoutBehaviorType.getTypeName(), timeoutBehaviorType);
            }
            TYPE_MAP = Collections.unmodifiableMap(validTypes);
        }

        /**
         * Find out if a string is a valid timeoutBehavior type or not
         *
         * @param type the string name
         * @return true if its valid, otherwise false
         */
        public static boolean isValidTimeoutBehaviorType(String type) {
            TimeoutBehaviorType timeoutBehaviorType = TYPE_MAP.get(type);
            return timeoutBehaviorType != null;
        }

        /**
         * Get the {@link TimeoutBehaviorType} corresponding to a name
         *
         * @param typeName the type name
         * @return the {@link TimeoutBehaviorType}
         */
        public static TimeoutBehaviorType getTimeoutBehaviorTypeFromName(String typeName) {
            return TYPE_MAP.get(typeName);
        }
    }

    /**
     * Default value for timeout behavior
     */
    public static final TimeoutBehaviorType DEFAULT_TIMEOUT_BEHAVIOR_TYPE = TimeoutBehaviorType.EXCEPTION;

    /**
     * Default value for properties.
     */
    public static final String DEFAULT_PROPERTIES = "";

    /**
     * Default value for property separator
     */
    public static final String DEFAULT_PROPERTY_SEPARATOR = ",";

    /**
     * Default value for timeout behavior
     */
    public static final String DEFAULT_VALUE = DEFAULT_TIMEOUT_BEHAVIOR_TYPE.getTypeName();

    private volatile TimeoutBehaviorType type = DEFAULT_TIMEOUT_BEHAVIOR_TYPE;
    private volatile String properties = DEFAULT_PROPERTIES;
    private volatile String propertySeparator = DEFAULT_PROPERTY_SEPARATOR;

    /**
     * Returns the type of timeout behavior configured
     *
     * @return the configured type
     */
    public String getType() {
        return type.getTypeName();
    }

    /**
     * Returns the type of timeout behavior configured
     *
     * @return the configured type
     */
    public TimeoutBehaviorType getTimeoutBehaviorType() {
        return type;
    }

    /**
     * Set the type of timeout behavior
     *
     * @param type
     */
    public void setType(String type) {
        if (!TimeoutBehaviorType.isValidTimeoutBehaviorType(type)) {
            throw new CacheException("Invalid value for timeoutBehavior type - '" + type + "'. Valid values are: '"
                    + TimeoutBehaviorType.EXCEPTION.getTypeName() + "',  '" + TimeoutBehaviorType.NOOP.getTypeName() + "',  '"
                    + TimeoutBehaviorType.LOCAL_READS.getTypeName());
        }
        this.type = TimeoutBehaviorType.getTimeoutBehaviorTypeFromName(type);
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
        if (properties == null) {
            throw new IllegalArgumentException("Properties cannot be null");
        }
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
        if (propertySeparator == null) {
            throw new IllegalArgumentException("Property Separator cannot be null");
        }
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((propertySeparator == null) ? 0 : propertySeparator.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TimeoutBehaviorConfiguration other = (TimeoutBehaviorConfiguration) obj;
        if (properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!properties.equals(other.properties)) {
            return false;
        }
        if (propertySeparator == null) {
            if (other.propertySeparator != null) {
                return false;
            }
        } else if (!propertySeparator.equals(other.propertySeparator)) {
            return false;
        }
        return type == other.type;
    }

    private Properties extractProperties() {
        final Properties rv = new Properties();
        final String propertiesString = this.properties;
        final String sep = this.propertySeparator;
        String[] props = propertiesString.split(propertySeparator);
        for (String prop : props) {
            String[] nvPair = prop.split("=");
            if (nvPair == null || nvPair.length != 2) {
                throw new InvalidConfigurationException("Property not specified correctly. Failed to parse: " + prop);
            }
            rv.setProperty(nvPair[0], nvPair[1]);
        }
        return rv;
    }

}
