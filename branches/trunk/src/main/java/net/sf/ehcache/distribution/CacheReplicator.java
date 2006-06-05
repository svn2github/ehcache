/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.distribution;

import net.sf.ehcache.event.CacheEventListener;

/**
 * Replicates cache entries to peers of the CacheManager
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public interface CacheReplicator extends CacheEventListener {

    /**
     * Returns whether update is through copy or invalidate
     * @return true if update is via copy, else false if invalidate
     */
    boolean isReplicateUpdatesViaCopy();

    /**
     * Returns whether the replicator is not active.
     * @return true if the status is not STATUS_ALIVE
     */
    boolean notAlive();

    /**
     * Checks that the replicator is is <code>STATUS_ALIVE</code>.
     * @return true if the replicator is is <code>STATUS_ALIVE</code>, else false.
     */
    boolean alive();

}
