/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;

/**
 * An Event Message, in respect of a particular cache.
 * <p/>
 * The message is Serializable, so that it can be sent across the network.
 * <p/>
 * The value of an Element is referenced with a SoftReference, so that a
 * value will fail to be delivered in preference to an OutOfMemory error.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class EventMessage implements Serializable {




    /**
     * A put or update event.
     */
    public static final int PUT = 0;

    /**
     * A remove or invalidate event.
     */
    public static final int REMOVE = 1;


    /**
     * A removeAll, which removes all elements from a cache
     */
    public static final int REMOVE_ALL = 3;

    private static final long serialVersionUID = -293616939110963629L;

    /**
     * The event component.
     */
    private final int event;

    /**
     * The element component. This is held by a SoftReference, so as to prevent
     * out of memory errors.
     */
    private transient SoftReference elementSoftReference;
    /**
     * The key component.
     */
    private final Serializable key;


    /**
     * Used to check if the value has been GCed
     */
    private final boolean wasElementNotNull;


    /**
     * Full constructor.
     *
     * @param event
     * @param key
     * @param element
     */
    public EventMessage(int event, Serializable key, Element element) {
        this.event = event;
        this.key = key;

        wasElementNotNull = element != null;
        elementSoftReference = new SoftReference(element);
    }

    /**
     * Gets the event.
     *
     * @return either {@link #PUT} or {@link #REMOVE}
     */
    public final int getEvent() {
        return event;
    }

    /**
     * @return the element component of the message. null if a {@link #REMOVE} event
     */
    public final Element getElement() {
        return (Element) elementSoftReference.get();
    }

    /**
     * @return the key component of the message. null if a {@link #PUT} event
     */
    public final Serializable getSerializableKey() {
        return key;
    }


    /**
     * @return true if because of SoftReference GC this EventMessage is no longer valid
     */
    public boolean isValid() {
        if (!wasElementNotNull) {
            return true;
        } else {
            return getElement() != null;
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        Element element = getElement();
        out.writeObject(element);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Element element = (Element) in.readObject();
        elementSoftReference = new SoftReference(element);
    }
}

