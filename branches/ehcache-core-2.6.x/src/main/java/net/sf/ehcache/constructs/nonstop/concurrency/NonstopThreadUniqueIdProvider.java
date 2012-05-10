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

package net.sf.ehcache.constructs.nonstop.concurrency;

/**
 * Utility class that returns a fixed unique ID for current thread
 *
 * @author asingh
 *
 */
abstract class NonstopThreadUniqueIdProvider {

    /**
     * Returns the fixed unique id for the current thread
     *
     * @return the fixed unique id for the current thread
     */
    public static long getCurrentNonstopThreadUniqueId() {
        // for all practical purposes, thread.getId() would do, though that can be reused
        return Thread.currentThread().getId();
    }

}
