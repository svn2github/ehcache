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

package net.sf.ehcache.constructs.nonstop.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;

/**
 * Enum encapsulating different {@link NonstopStore} types for use as timeoutBehavior by NonstopStore
 *
 * @author Abhishek Sanoujam
 *
 */
public enum NonstopTimeoutBehaviorType {

    /**
     * Type encapsulating {@link NonstopStore} which throws exception for all timed out operations
     */
    EXCEPTION_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonstopStore newTimeoutStore(final NonstopActiveDelegateHolder nonstopActiveDelegateHolder) {
            return ExceptionOnTimeoutStore.getInstance();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns {@link NonstopTimeoutBehaviorType#EXCEPTION_CONFIG_PROPERTY_NAME}
         */
        @Override
        public String getConfigPropertyName() {
            return EXCEPTION_CONFIG_PROPERTY_NAME;
        }

    },
    /**
     * Type encapsulating {@link NonstopStore} which returns null for get operations and does nothing
     * for put's and remove's
     */
    NO_OP_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonstopStore newTimeoutStore(final NonstopActiveDelegateHolder nonstopActiveDelegateHolder) {
            return NoOpOnTimeoutStore.getInstance();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns {@link NonstopTimeoutBehaviorType#NO_OP_CONFIG_PROPERTY_NAME}
         */
        @Override
        public String getConfigPropertyName() {
            return NO_OP_CONFIG_PROPERTY_NAME;
        }

    },
    /**
     * Type encapsulating {@link NonstopStore} which returns whatever local value is associated with the
     * key for get operations and does nothing for put's and remove's. Works only Terracotta clustered caches
     */
    LOCAL_READS_ON_TIMEOUT() {

        /**
         * {@inheritDoc}
         */
        @Override
        public NonstopStore newTimeoutStore(final NonstopActiveDelegateHolder nonstopActiveDelegateHolder) {
            return new LocalReadsOnTimeoutStore(nonstopActiveDelegateHolder);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns {@link NonstopTimeoutBehaviorType#LOCAL_READS_CONFIG_PROPERTY_NAME}
         */
        @Override
        public String getConfigPropertyName() {
            return LOCAL_READS_CONFIG_PROPERTY_NAME;
        }

    };

    /**
     * Creates and returns new instance of {@link NonstopStore} for this type
     *
     * @param nonstopUnderlyingStoreResolver
     * @return new instance of {@link NonstopStore} for this type
     */
    public abstract NonstopStore newTimeoutStore(final NonstopActiveDelegateHolder nonstopUnderlyingStoreResolver);

    /**
     * Name to be used for this type. This value is used for "timeoutBehavior" key when configuring NonstopStore
     *
     * @return name to be used for the timeout behavior
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

    private static final Map<String, NonstopTimeoutBehaviorType> NAME_TYPE_MAP = new HashMap<String, NonstopTimeoutBehaviorType>();

    static {
        for (NonstopTimeoutBehaviorType type : NonstopTimeoutBehaviorType.values()) {
            NAME_TYPE_MAP.put(type.getConfigPropertyName(), type);
        }
    }

    /**
     * Return a {@link NonstopTimeoutBehaviorType} for the string property name.
     *
     * @param configName
     * @return {@link NonstopTimeoutBehaviorType} for the string property name.
     * @throws IllegalArgumentException
     *             if the passed in configName is <b>NOT</b> one of:
     *             <ul>
     *             <li>{@link #EXCEPTION_CONFIG_PROPERTY_NAME}</li>
     *             <li>{@link #NO_OP_CONFIG_PROPERTY_NAME}</li>
     *             <li>{@link #LOCAL_READS_CONFIG_PROPERTY_NAME}</li>
     *             </ul>
     */
    public static NonstopTimeoutBehaviorType getTypeFromConfigPropertyName(String configName) throws IllegalArgumentException {
        NonstopTimeoutBehaviorType type = NAME_TYPE_MAP.get(configName);
        if (type == null) {
            throw new IllegalArgumentException("Unrecognized timeoutBehavior config property value - " + configName);
        }
        return type;
    }

    /**
     * Check if a given value is a valid timeoutBehavior or not
     *
     * @param value the value to check
     * @return true is the value is valid, false otherwise
     */
    public static boolean isValidTimeoutBehaviorType(String value) {
        NonstopTimeoutBehaviorType type = NAME_TYPE_MAP.get(value);
        return type != null;
    }

    /**
     * Returns set of all valid timeoutBehavior values
     *
     * @return set of all valid timeoutBehavior values
     */
    public static Set<String> getValidTimeoutBehaviors() {
        return NAME_TYPE_MAP.keySet();
    }
}
