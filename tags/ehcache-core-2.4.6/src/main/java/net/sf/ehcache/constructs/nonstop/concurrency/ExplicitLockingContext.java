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

package net.sf.ehcache.constructs.nonstop.concurrency;

/**
 * Package protected interface to maintain explicit locking state
 *
 * @author Abhishek Sanoujam
 *
 */
interface ExplicitLockingContext {

    /**
     * Returns true if the current thread has acquired any locks previously and not released yet
     *
     * @return true if the current thread has acquired any locks previously and not released yet
     */
    public boolean areAnyExplicitLocksAcquired();

    /**
     * Mark a lock acquire
     *
     * @param currentNonstopThreadUniqueId
     */
    public void lockAcquired(long currentNonstopThreadUniqueId);

    /**
     * Mark a lock release
     */
    public void lockReleased();

    /**
     * Returns true if some other thread than the thread whose unique id is passed in the parameter has acquired any locks previously and
     * not released yet
     *
     * @param currentNonstopThreadUniqueId
     * @return true if some other thread than the thread whose unique id is passed in the parameter has acquired any locks previously and
     *         not released yet
     */
    public boolean areLocksAcquiredByOtherThreads(long currentNonstopThreadUniqueId);
}
