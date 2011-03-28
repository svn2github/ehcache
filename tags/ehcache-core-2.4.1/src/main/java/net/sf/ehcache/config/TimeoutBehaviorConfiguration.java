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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.constructs.nonstop.NonstopTimeoutBehaviorFactory;
import net.sf.ehcache.constructs.nonstop.store.ExceptionOnTimeoutStore;
import net.sf.ehcache.constructs.nonstop.store.LocalReadsOnTimeoutStore;
import net.sf.ehcache.constructs.nonstop.store.NoOpOnTimeoutStore;
import net.sf.ehcache.util.ClassLoaderUtil;

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

            /**
             * {@inheritDoc}
             */
            @Override
            public NonstopTimeoutBehaviorFactory getTimeoutBehaviorFactory(Properties properties) {
                return ExceptionOnTimeoutStore.FACTORY;
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

            /**
             * {@inheritDoc}
             */
            @Override
            public NonstopTimeoutBehaviorFactory getTimeoutBehaviorFactory(Properties properties) {
                return NoOpOnTimeoutStore.FACTORY;
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

            /**
             * {@inheritDoc}
             */
            @Override
            public NonstopTimeoutBehaviorFactory getTimeoutBehaviorFactory(Properties properties) {
                return LocalReadsOnTimeoutStore.FACTORY;
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

            /**
             * {@inheritDoc}
             */
            @Override
            public NonstopTimeoutBehaviorFactory getTimeoutBehaviorFactory(Properties properties) {
                if (properties == null || !properties.containsKey(CUSTOM_TYPE_FACTORY_PROPERTY_NAME)) {
                    throw new CacheException("When using " + getTypeName() + " timeout behavior type, need to set properties with key '"
                            + CUSTOM_TYPE_FACTORY_PROPERTY_NAME + "', specified properties: " + (properties == null ? "NULL" : properties));
                }
                final String customFactoryClassName = properties.getProperty(CUSTOM_TYPE_FACTORY_PROPERTY_NAME);
                Object factory = ClassLoaderUtil.createNewInstance(customFactoryClassName);
                if (!(factory instanceof NonstopTimeoutBehaviorFactory)) {
                    throw new CacheException("The factory '" + customFactoryClassName + "' is NOT an instance of "
                            + NonstopTimeoutBehaviorFactory.class.getName());
                }
                return (NonstopTimeoutBehaviorFactory) factory;
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
        public abstract NonstopTimeoutBehaviorFactory getTimeoutBehaviorFactory(Properties properties);

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
    public final static TimeoutBehaviorType DEFAULT_TIMEOUT_BEHAVIOR_TYPE = TimeoutBehaviorType.EXCEPTION;

    /**
     * Default value for properties.
     */
    public final static String DEFAULT_PROPERTIES = "";

    /**
     * Default value for property separator
     */
    public final static String DEFAULT_PROPERTY_SEPARATOR = ",";
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

    /**
     * Get the {@link NonstopTimeoutBehaviorFactory} according to the active config
     *
     * @return the nonstopTimeoutBehaviorFactory
     */
    public NonstopTimeoutBehaviorFactory getNonstopTimeoutBehaviorFactory() {
        switch (type) {
            case EXCEPTION:
            case NOOP:
            case LOCAL_READS:
                // no need to parse properties as not used (for now at least)
                return type.getTimeoutBehaviorFactory(null);
            case CUSTOM:
                return type.getTimeoutBehaviorFactory(extractProperties());
            default:
                throw new CacheException("Unknown timeout behavior type - " + type);
        }
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
