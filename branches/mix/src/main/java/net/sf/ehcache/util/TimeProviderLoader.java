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
package net.sf.ehcache.util;

/**
 * Loader for the TimeProvider implementation that will be used in SlewClock
 * Changing this before loading the SlewClock class enables you to use some custom {@link SlewClock.TimeProvider} implementation.
 * The default is {@link System#currentTimeMillis()}
 *
 * @author Alex Snaps
 * @see SlewClock.TimeProvider
 */
final class TimeProviderLoader {

    private static SlewClock.TimeProvider timeProvider = new SlewClock.TimeProvider() {
        public final long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    };

    private TimeProviderLoader() {
        // Do not instantiate me!
    }

    /**
     * Getter
     * @return the currently set timeProvider
     */
    public static synchronized SlewClock.TimeProvider getTimeProvider() {
        return timeProvider;
    }

    /**
     * Setter, needs to be set before the {@link SlewClock} class is being loaded!
     * @param timeProvider the {@link SlewClock.TimeProvider} implementation to use.
     */
    public static synchronized void setTimeProvider(final SlewClock.TimeProvider timeProvider) {
        TimeProviderLoader.timeProvider = timeProvider;
    }
}
