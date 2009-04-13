/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

import java.io.Serializable;

import net.sf.ehcache.distribution.EventMessage;

/**
 * Serializable type used for Jgroups type replication
 * 
 * @author Pierre Monestie (pmonestie__REMOVE__THIS__@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class JGroupSerializable implements Serializable {

    /**
     * A put or update event.
     */
    public static final int PUT = EventMessage.PUT;

    /**
     * A remove or invalidate event.
     */
    public static final int REMOVE = EventMessage.REMOVE;

    /**
     * A removeAll, which removes all elements from a cache
     */
    public static final int REMOVE_ALL = EventMessage.REMOVE_ALL;
    
    public static final int ASK_FOR_BOOTSTRAP = JGroupEventMessage.ASK_FOR_BOOTSTRAP;
    
    public static final int BOOTSTRAP_REPLY = JGroupEventMessage.BOOTSTRAP_REPLY;

    private int event;

    private Serializable key;

    private Object value;

    private String cacheName;

    /**
     * @param event the type of replication event
     * @param key the key
     * @param value can be null if REMOVE or REMOVE_ALL
     */
    public JGroupSerializable(int event, Serializable key, Object value, String cacheName) {
        super();
        this.event = event;
        this.key = key;
        this.value = value;
        this.cacheName = cacheName;
    }

    /**
     * Gets the event
     * 
     * @return the event
     */
    public int getEvent() {
        return event;
    }

    /**
     * Get the Serializable key for the event
     * 
     * @return the key
     */
    public Serializable getKey() {
        return key;
    }

    /**
     * Gets the value, null if REMOVE or REMOVE_ALL
     * 
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Gets the cache name
     * 
     * @return the cache name
     */
    public String getCacheName() {
        return cacheName;
    }

}
