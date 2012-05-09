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

package net.sf.ehcache.distribution;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.event.CacheManagerEventListener;

import java.util.List;

/**
 * A listener for updates, which controls remote cache peers.
 * @author Greg Luck
 * @version $Id$
 */
public interface CacheManagerPeerListener extends CacheManagerEventListener {

    /**
     * All of the caches which are listening for remote changes.
     * @return a list of <code>CachePeer</code> objects
     */
    List getBoundCachePeers();



    /**
     * A listener will normally have a resource that only one instance can use at the same time,
     * such as a port. This identifier is used to tell if it is unique and will not conflict with an
     * existing instance using the resource.
     * @return a String identifier for the resource
     */
    String getUniqueResourceIdentifier();


    /**
     * If a conflict is detected in unique resource use, this method signals the listener to attempt
     * automatic resolution of the resource conflict.
     * @throws IllegalStateException if the statis of the listener is not {@link net.sf.ehcache.Status#STATUS_UNINITIALISED}
     */
    void attemptResolutionOfUniqueResourceConflict() throws IllegalStateException, CacheException;

    /**
     * The replication scheme this listener interacts with.
     * Each peer provider has a scheme name, which can be used by caches to specify for replication and bootstrap purposes.
     * @return the well-known scheme name, which is determined by the replication provider author.
     */
    String getScheme();
}
