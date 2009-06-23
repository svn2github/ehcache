/**
 *  Copyright 2003-2009 Luck Consulting Pty Ltd
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

package net.sf.ehcache.distribution.jgroups;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.EventMessage;

import java.io.Serializable;

/**
 * An EventMessage used for JGroups
 * @author Pierre Monestie (pmonestie[at]@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 *          EventMessage class for the JGroupsCacheReplicator.
 */
public class JGroupEventMessage extends EventMessage {

    /**
     * Request for bootstrap
     */
    public static final int ASK_FOR_BOOTSTRAP = 10;

    /**
     * Reply to bootstrap 
     */
    public static final int BOOTSTRAP_REPLY = 11;

    private String cacheName;

    private transient Ehcache cache;

    /**
     * An event message for the JGroupsCacheReplicator. We keep as transient the
     * origin cache and we serialize the cacheName. That way the JgroupManager
     * will know from which cache the message came from
     * 
     * @param event
     *            (PUT,REMOVE,REMOVE_ALL)
     * @param key
     *            the serializable key of the cache element
     * @param element
     *            The element itself. In case of a put.
     * @param cache
     *            the Ehcache instance. This is a transient variable
     * @param cacheName
     *            the name of the cache
     */
    public JGroupEventMessage(int event, Serializable key, Element element, Ehcache cache, String cacheName) {
        super(event, key, element);
        this.cache = cache;
        this.cacheName = cacheName;

    }

    /**
     * @return the cache from which this event originated
     */
    public Ehcache getCache() {
        return cache;
    }

    /**
     * Returns the cache name
     * 
     * @return the cache name
     */
    public String getCacheName() {
        return cacheName;
    }

}
