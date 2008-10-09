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

import javax.jms.Session;
import java.io.Serializable;


/**
 * A subclass of EventMessage which describes all of the ehcache distributed message types in a way
 * that works for JMS.
 *
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSEventMessage extends EventMessage {

    /**
     * Actions that tie in with EventMessage actions. EventMessage has not been updated to use enums.
     */
    public enum Action {

        PUT(EventMessage.PUT),
        REMOVE(EventMessage.REMOVE),
        REMOVE_ALL(EventMessage.REMOVE_ALL),
        GET(10);

        private int action;

        public static Action forString(String value) {
            for (Action action : values()) {
                if (action.name().equals(value)) {
                    return action;
                }
            }
            return null;
        }

        private Action(int mode) {
            this.action = mode;
        }

        public int toInt() {
            return action;
        }
    }


    /**
     * A JMS message property which contains the name of the cache to operate on.
     */
    public static final String CACHE_NAME_PROPERTY = "cacheName";

    /**
     * A JMS message property which contains the mimeType of the message.
     * Applies to the <code>PUT</code> action. If not set the message is interpreted as follows:
     * <ul>
     * <li>ObjectMessage - if it is an net.sf.ehcache.Element, then it is treated as such and stored in the cache. All others
     * are stored in the cache as value of  MimeTypeByteArray. The mimeType is stored as type of <code>application/x-java-serialized-object</code>.
     * When the ObjectMessage is of type net.sf.ehcache.Element, the mimeType is ignored. 
     * <li>TextMessage - Stored in the cache as value of MimeTypeByteArray. The mimeType is stored as type of <code>text/plain</code>.
     * <li>BytesMessage - Stored in the cache as value of MimeTypeByteArray. The mimeType is stored as type of <code>application/octet-stream</code>.
     * </ol>
     * Other message types are not supported.
     * <p/>
     * To send XML use a TextMessage and set the mimeType to <code>application/xml</code>.It will be stored in the cache
     * as a value of MimeTypeByteArray.
     */
    public static final String MIME_TYPE_PROPERTY = "mimeType";

    /**
     * A JMS message property which contains the action to perform on the cache.
     * Available actions are <code>PUT</code>, <code>REMOVE</code> and <code>REMOVE_ALL</code>.
     * If not set no action is performed.
     */
    public static final String ACTION_PROPERTY = "action";

    /**
     * The key in the cache on which to operate on.
     * The <code>REMOVE_ALL</code> action does not require a key.
     * <p/>
     * If an ObjectMessage of type net.sf.ehcache.Element is sent, the key is contained in the element. Any key
     * set as a property is ignored.
     */
    public static final String KEY_PROPERTY = "key";

    private static final long serialVersionUID = 927345728947584L;

    private String cacheName;

    /**
     * @param event     one of the types from EventMessage
     * @param key       the key of the Element. May be null for the removeAll message type
     * @param element   may be null for removal and invalidation message types
     * @param cacheName the name of the cache in the CacheManager.
     */
    public JMSEventMessage(int event, Serializable key, Element element, String cacheName) {
        super(event, key, element);
        setCacheName(cacheName);
    }

    /**
     * Returns the cache name
     *
     * @return the cache name in the CacheManager.
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * Sets the cache name.
     *
     * @param cacheName the name of the cache in the CacheManager
     */
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }


    /**
     * Returns the message as a String
     *
     * @return a String represenation of the message
     */
    @Override
    public String toString() {
        return "JMSEventMessage ( event = " + getEvent() + ", element = " + getElement() + ", cacheName = " + cacheName;
    }

}
