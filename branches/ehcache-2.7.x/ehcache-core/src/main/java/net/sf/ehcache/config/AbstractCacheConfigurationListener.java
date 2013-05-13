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
 * @author Alex Snaps
 */
public abstract class AbstractCacheConfigurationListener implements CacheConfigurationListener {
    /**
     * {@inheritDoc}
     */
    public void timeToIdleChanged(final long oldTimeToIdle, final long newTimeToIdle) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void timeToLiveChanged(final long oldTimeToLive, final long newTimeToLive) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void diskCapacityChanged(final int oldCapacity, final int newCapacity) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void memoryCapacityChanged(final int oldCapacity, final int newCapacity) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void loggingChanged(final boolean oldValue, final boolean newValue) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void registered(final CacheConfiguration config) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void deregistered(final CacheConfiguration config) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void maxBytesLocalHeapChanged(final long oldValue, final long newValue) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void maxBytesLocalDiskChanged(final long oldValue, final long newValue) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void maxEntriesInCacheChanged(long oldCapacity, long newCapacity) {
        //
    }


}
