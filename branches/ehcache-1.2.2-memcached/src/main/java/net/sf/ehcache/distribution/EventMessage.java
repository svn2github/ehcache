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

import net.sf.ehcache.Element;

import java.io.Serializable;

/**
 * An Event Message, in respect of a particular cache.
 * <p/>
 * The message is Serializable, so that it can be sent across the network.
 * @author Greg Luck
 * @version $Id$
 * @noinspection SerializableHasSerializationMethods
 */
public final class EventMessage implements Serializable {


    /**
     * A put or update event.
     */
    public static final int PUT = 0;

    /**
     * A remove or invalidate event.
     */
    public static final int REMOVE = 1;

    private static final long serialVersionUID = -5760542938372164184L;
    
    /**
     * The event component.
     */
    private final int event;
    /**
     * The element component.
     */
    private final Element element;
    /**
     * The key component.
     */
    private final Serializable key;



    /**
     * Full constructor.
     * @param event
     * @param key
     * @param element
     */
    public EventMessage(int event, Serializable key, Element element) {
        this.event = event;
        this.key = key;
        this.element = element;
    }

    /**
     * Gets the event.
     * @return either {@link #PUT} or {@link #REMOVE}
     */
    public final int getEvent() {
        return event;
    }

    /**
     * @return the element component of the message. null if a {@link #REMOVE} event
     */
    public final Element getElement() {
        return element;
    }

    /**
     * @return the key component of the message. null if a {@link #PUT} event
     */
    public final Serializable getSerializableKey() {
        return key;
    }
}

