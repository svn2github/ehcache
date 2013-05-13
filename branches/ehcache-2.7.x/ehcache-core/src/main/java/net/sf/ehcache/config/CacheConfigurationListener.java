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
 * Instances of CacheConfigurationListener can be registered with CacheConfiguration
 * instances in order to receive notification when any of the dynamic properties of
 * the configuration are changed.
 *
 * @author Chris Dennis
 */
public interface CacheConfigurationListener {

    /**
     * Indicates a change in the configurations time to idle
     *
     * @param oldTimeToIdle previous time to idle value
     * @param newTimeToIdle new time to idle value
     */
    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle);

    /**
     * Indicates a change in the configurations time to live
     *
     * @param oldTimeToLive previous time to live value
     * @param newTimeToLive new time to live value
     */
    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive);

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

    /**
     * Indicates a change in the configuration for enable/disable logging
     * @param oldValue old value whether logging was enabled or not
     * @param newValue new value whether logging was enabled or not
     */
    public void loggingChanged(boolean oldValue, boolean newValue);

    /**
     * Indicates that this listener was registered with the given configuration
     *
     * @param config
     */
    public void registered(CacheConfiguration config);

    /**
     * Indicates that this listener was removed from the given configuration
     *
     * @param config
     */
    public void deregistered(CacheConfiguration config);

    /**
     * Indicates a change in the configuration for maxBytesLocalHeap setting
     * @param oldValue old value in bytes
     * @param newValue new value in bytes
     */
    public void maxBytesLocalHeapChanged(long oldValue, long newValue);

    /**
     * Indicates a change in the configuration for maxBytesLocalDisk setting
     * @param oldValue old value in bytes
     * @param newValue new value in bytes
     */
    public void maxBytesLocalDiskChanged(long oldValue, long newValue);

    /**
     * Indicates a change in the configuration for maxEntriesInCache setting
     * @param oldValue old value
     * @param newValue new value
     */
    public void maxEntriesInCacheChanged(long oldValue, long newValue);
}
