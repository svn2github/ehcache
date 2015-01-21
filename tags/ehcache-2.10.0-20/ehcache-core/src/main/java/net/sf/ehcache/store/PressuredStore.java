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
package net.sf.ehcache.store;

import java.util.concurrent.Callable;

/**
 * A store that can be under pressure... you wouldn't want your store to be under pressure, now would you ?
 *
 * @author Alex Snaps
 */
public interface PressuredStore {

    /**
     * Registers an emergency valve
     * @param valve
     */
    void registerEmergencyValve(Callable<Void> valve);
}
