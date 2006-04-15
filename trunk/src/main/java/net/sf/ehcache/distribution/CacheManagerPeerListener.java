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

import net.sf.ehcache.CacheException;

import java.util.List;

/**
 * A listener for updates, which controls remote cache peers
 * @author Greg Luck
 * @version $Id$
 */
public interface CacheManagerPeerListener {


    /**
     * Call to start the listeners and do any other network initialisation.
     */
    void init() throws CacheException;

    /**
     * Stop the listener and free any resources.
     */
    void dispose() throws CacheException;

    /**
     * All of the caches which are listenting for remote changes.
     * @return a list of <code>CachePeer</code> objects
     */
    List getBoundCachePeers();



}
