/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
 *
 * @author cdennis
 */
public interface CacheConfigurationListener {

    /**
     * Indicates a change in the configurations TTI
     *
     * @param oldTti previous TTI value
     * @param newTti new TTI value
     */
    public void timeToIdleChanged(long oldTti, long newTti);

    /**
     * Indicates a change in the configurations TTL
     *
     * @param oldTtl previous TTL value
     * @param newTtl new TTL value
     */
    public void timeToLiveChanged(long oldTtl, long newTtl);

    /**
     * Indicates a change in the configurations disk store capacity
     *
     * @param oldCapacity previous capacity
     * @param newCapacity new capacity
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity);

    /**
     * Indicates a change in the configurations memory store capacity
     *
     * @param oldCapacity previous capacity
     * @param newCapacity new capacity
     */
    public void memoryCapacityChanged(int oldCapacity, int newCapacity);

}
