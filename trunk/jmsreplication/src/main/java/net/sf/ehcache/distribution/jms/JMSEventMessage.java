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

package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.EventMessage;

import java.io.Serializable;


/**
 * A subclass of EventMessage which describes all of the ehcache distributed message types in a way
 * that works for JMS.
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSEventMessage extends EventMessage {

    private static final long serialVersionUID = 927345728947584L;

    private String cacheName;

    /**
     *
     * @param event one of the types from EventMessage
     * @param key the key of the Element. May be null for the removeAll message type
     * @param element may be null for removal and invalidation message types
     * @param cacheName the name of the cache in the CacheManager.
     */
    public JMSEventMessage(int event, Serializable key, Element element, String cacheName) {
        super(event, key, element);
        setCacheName(cacheName);
    }

    /**
     * Returns the cache name
     * @return the cache name in the CacheManager.
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * Sets the cache name.
     * @param cacheName the name of the cache in the CacheManager
     */
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }


    /**
     * Returns the message as a String
     * @return a String represenation of the message
     */
    @Override
    public String toString() {
        return "JMSEventMessage ( event = " + getEvent() + ", element = " + getElement() + ", cacheName = " + cacheName;
    }

}
