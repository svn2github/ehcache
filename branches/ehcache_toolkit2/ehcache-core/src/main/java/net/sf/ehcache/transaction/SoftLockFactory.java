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

package net.sf.ehcache.transaction;

/**
 * A factory of soft-locks supporting a specific isolation level.
 *
 * @author Chris Dennis
 */
public interface SoftLockFactory {

    /**
     * Construct a new softlock to be managed by the given manager for a specific key.
     *
     * @param manager soft lock manager
     * @param key key to generate against
     * @return a new soft lock
     */
    SoftLock newSoftLock(SoftLockManager manager, Object key);

}
