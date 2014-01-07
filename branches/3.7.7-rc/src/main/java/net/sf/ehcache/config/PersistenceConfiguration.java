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

/**
 * Class to hold the persistence policy configuration.
 *
 * @author Chris Dennis
 */
public class PersistenceConfiguration {

    /**
     * Default synchronous writes setting
     */
    public static final boolean DEFAULT_SYNCHRONOUS_WRITES = false;

    /**
     * Enumeration of the legal persistence strategies
     */
    public static enum Strategy {
        /**
         * Standard open source (non fault-tolerant) on-disk persistence
         */
        LOCALTEMPSWAP,
        /**
         * Enterprise fault tolerant persistence
         */
        LOCALRESTARTABLE,
        /**
         * No persistence
         */
        NONE,
        /**
         * Terracotta clustered persistence (requires a Terracotta clustered cache).
         */
        DISTRIBUTED;
    }

    private volatile Strategy strategy;
    private volatile boolean synchronousWrites;

    /**
     * Gets the persistence strategy
     *
     * @return the persistence strategy
     */
    public Strategy getStrategy() {
        return strategy;
    }

    /**
     * Sets the persistence strategy
     *
     * @param strategy the persistence strategy
     */
    public void setStrategy(String strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("strategy must be non-null");
        }
        strategy(Strategy.valueOf(strategy.toUpperCase()));
    }

    /**
     * Builder method to set the persistence strategy
     *
     * @param strategy the persistence strategy
     * @return this PersistenceConfiguration object
     */
    public PersistenceConfiguration strategy(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * Builder method to set the persistence strategy using a String object
     *
     * @param strategy the persistence strategy
     * @return this PersistenceConfiguration object
     */
    public PersistenceConfiguration strategy(String strategy) {
        setStrategy(strategy);
        return this;
    }

    /**
     * Gets the persistence write mode
     *
     * @return the persistence write mode
     */
    public boolean getSynchronousWrites() {
        return synchronousWrites;
    }

    /**
     * Sets the persistence write mode
     *
     * @param synchronousWrites the persistence write mode
     */
    public void setSynchronousWrites(boolean synchronousWrites) {
        this.synchronousWrites = synchronousWrites;
    }

    /**
     * Builder method to set the persistence write mode
     *
     * @param synchronousWrites the persistence write mode
     * @return this PersistenceConfiguration object
     */
    public PersistenceConfiguration synchronousWrites(boolean synchronousWrites) {
        setSynchronousWrites(synchronousWrites);
        return this;
    }


}
