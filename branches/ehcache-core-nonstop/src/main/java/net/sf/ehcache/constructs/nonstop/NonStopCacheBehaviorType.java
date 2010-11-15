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

package net.sf.ehcache.constructs.nonstop;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.nonstop.behavior.ExceptionOnTimeoutBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.LocalReadsBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.NoOpOnTimeoutBehavior;

/**
 * Enum encapsulating different {@link NonStopCacheBehavior} types used by {@link NonStopCache}
 *
 * @author Abhishek Sanoujam
 *
 */
public enum NonStopCacheBehaviorType {

    /**
     * {@link NonStopCacheBehaviorType} encapsulating {@link NonStopCacheBehavior} which throws exception for all timed out operations
     */
    EXCEPTION_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonStopCacheBehavior newCacheBehavior(final Ehcache ehcache) {
            return ExceptionOnTimeoutBehavior.getInstance();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns {@link NonStopCacheBehaviorType#EXCEPTION_CONFIG_PROPERTY_NAME}
         */
        @Override
        public String getConfigPropertyName() {
            return EXCEPTION_CONFIG_PROPERTY_NAME;
        }

    },
    /**
     * {@link NonStopCacheBehaviorType} encapsulating {@link NonStopCacheBehavior} which returns null for get operations and does nothing
     * for put's and remove's
     */
    NO_OP_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonStopCacheBehavior newCacheBehavior(final Ehcache ehcache) {
            return NoOpOnTimeoutBehavior.getInstance();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns {@link NonStopCacheBehaviorType#NO_OP_CONFIG_PROPERTY_NAME}
         */
        @Override
        public String getConfigPropertyName() {
            return NO_OP_CONFIG_PROPERTY_NAME;
        }

    },
    /**
     * {@link NonStopCacheBehaviorType} encapsulating {@link NonStopCacheBehavior} which returns whatever local value is associated with the
     * key for get operations and does nothing for put's and remove's. Works only when decorating {@link Cache} instances clustered with
     * Terracotta.
     */
    LOCAL_READS_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonStopCacheBehavior newCacheBehavior(final Ehcache ehcache) {
            if (!(ehcache instanceof Cache)) {
                throw new UnsupportedOperationException(LOCAL_READS_ON_TIMEOUT.name() + " behavior is only supported for "
                        + Cache.class.getName() + " instances.");
            }
            return new LocalReadsBehavior((Cache) ehcache);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns {@link NonStopCacheBehaviorType#LOCAL_READS_CONFIG_PROPERTY_NAME}
         */
        @Override
        public String getConfigPropertyName() {
            return LOCAL_READS_CONFIG_PROPERTY_NAME;
        }

    };

    /**
     * Creates and returns new instance of {@link NonStopCacheBehavior} for this type
     *
     * @param ehcache
     * @return new instance of {@link NonStopCacheBehavior} for this type
     */
    public abstract NonStopCacheBehavior newCacheBehavior(Ehcache ehcache);

    /**
     * Name to be used for this type. This value is used for "timeoutBehavior" key when configuring {@link NonStopCache} with
     * java.util.Properties
     *
     * @return new instance of {@link NonStopCacheBehavior} for this type
     */
    public abstract String getConfigPropertyName();

    /**
     * Value for {@link #EXCEPTION_ON_TIMEOUT} behavior. This value is used for "timeoutBehavior" key when configuring NonStopCache with
     * java.util.Properties.
     */
    public static final String EXCEPTION_CONFIG_PROPERTY_NAME = "exception";
    /**
     * Value for {@link #NO_OP_ON_TIMEOUT} behavior. This value is used for "timeoutBehavior" key when configuring NonStopCache with
     * java.util.Properties.
     */
    public static final String NO_OP_CONFIG_PROPERTY_NAME = "noop";
    /**
     * Value for {@link #LOCAL_READS_ON_TIMEOUT} behavior. This value is used for "timeoutBehavior" key when configuring NonStopCache with
     * java.util.Properties.
     */
    public static final String LOCAL_READS_CONFIG_PROPERTY_NAME = "localReads";

    private static Map<String, NonStopCacheBehaviorType> configNameToTypeMapping = new HashMap<String, NonStopCacheBehaviorType>();

    static {
        for (NonStopCacheBehaviorType type : NonStopCacheBehaviorType.values()) {
            configNameToTypeMapping.put(type.getConfigPropertyName(), type);
        }
    }

    /**
     * Return a {@link NonStopCacheBehaviorType} for the string property name.
     *
     * @param configName
     * @return {@link NonStopCacheBehaviorType} for the string property name.
     * @throws IllegalArgumentException
     *             if the passed in configName is <b>NOT</b> one of:
     *             <ul>
     *             <li>{@link #EXCEPTION_CONFIG_PROPERTY_NAME}</li>
     *             <li>{@link #NO_OP_CONFIG_PROPERTY_NAME}</li>
     *             <li>{@link #LOCAL_READS_CONFIG_PROPERTY_NAME}</li>
     *             </ul>
     */
    public static NonStopCacheBehaviorType getTypeFromConfigPropertyName(String configName) throws IllegalArgumentException {
        NonStopCacheBehaviorType type = configNameToTypeMapping.get(configName);
        if (type == null) {
            throw new IllegalArgumentException("Unrecognized NonStopCacheBehaviorType config property value -- " + configName);
        }
        return type;
    }

    /**
     * Check if a given value is a valid timeoutBehavior or not
     *
     * @param value the value to check
     * @return true is the value is valid, false otherwise
     */
    public static boolean isValidTimeoutValue(String value) {
        NonStopCacheBehaviorType type = configNameToTypeMapping.get(value);
        return type != null;
    }

    /**
     * Returns set of all valid timeoutBehavior values
     *
     * @return set of all valid timeoutBehavior values
     */
    public static Set<String> getValidTimeoutBehaviors() {
        return configNameToTypeMapping.keySet();
    }
}
