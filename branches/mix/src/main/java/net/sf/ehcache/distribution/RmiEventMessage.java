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

import java.io.Serializable;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 *
 * @author cdennis
 */
public final class RmiEventMessage extends EventMessage {

    /**
     * Enumeration of event types.
     */
    public enum RmiEventType {

        /**
         * A put or update event.
         */
        PUT,
        
        /**
         * A remove or invalidate event.
         */
        REMOVE,
        
        /**
         * A removeAll, which removes all elements from a cache
         */
        REMOVE_ALL;
    }

    /**
     * The event component.
     */
    private final RmiEventType type;

    /**
     * The element component.
     */
    private final Element element;

    /**
     * Full constructor.
     *
     * @param cache
     * @param type
     * @param key
     * @param element
     */
    public RmiEventMessage(Ehcache cache, RmiEventType type, Serializable key, Element element) {
        super(cache, key);
        this.type = type;
        this.element = element;
    }
    
    /**
     * Gets the event.
     *
     * @return either {@link #PUT} or {@link #REMOVE}
     */
    public final RmiEventType getType() {
        return type;
    }

    /**
     * @return the element component of the message. null if a {@link #REMOVE} event
     */
    public final Element getElement() {
        return element;
    }
}
