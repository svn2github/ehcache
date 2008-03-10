/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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
 * @author Pierre Monestie (pmonestie__REMOVE__THIS__@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class JGroupSerializable implements Serializable {
    int event;

    Serializable key;

    Object value;
    String cacheName;

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

    /**
     * @param event the type of replication event
     * @param key   the key
     * @param value can be null if REMOVE or REMOVE_ALL
     */
    public JGroupSerializable(int event, Serializable key, Object value, String cacheName) {
        super();
        this.event = event;
        this.key = key;
        this.value = value;
        this.cacheName = cacheName;
    }

    public int getEvent() {
        return event;
    }

    public Serializable getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public String getCacheName() {
        return cacheName;
    }

}
