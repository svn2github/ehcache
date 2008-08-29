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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.CachePeer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSCachePeer implements CachePeer, MessageListener {

    private static final Logger LOG = Logger.getLogger(JMSCachePeer.class.getName());

    private Session producerSession;
    private CacheManager cacheManager;
    private MessageProducer messageProducer;
    private final String nodeName;

    /**
     * 
     * @param cacheManager
     * @param messageProducer
     * @param producerSession
     * @param nodeName
     */
    public JMSCachePeer(CacheManager cacheManager, MessageProducer messageProducer, Session producerSession,
                        String nodeName) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("JMSCachePeer constructor ( cacheManager = "
                    + cacheManager
                    + ", messageProducer = " + messageProducer + ", nodeName = " + nodeName + " ) called");
        }

        this.cacheManager = cacheManager;
        this.messageProducer = messageProducer;
        this.producerSession = producerSession;
        this.nodeName = nodeName;

    }

    private synchronized void handleNotification(JMSEventMessage message) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("handleNotification ( message = " + message + " ) called ");
        }

        int event = message.getEvent();
        Cache cache;
        String name = message.getCacheName();
        try {
            cache = cacheManager.getCache(name);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cache {0} not found.", name);
            return;
        }
        Element element = message.getElement();

        switch (event) {
            case JMSEventMessage.PUT:
                put(cache, element);
                break;
            case JMSEventMessage.REMOVE:
                remove(cache, element);
                break;
            case JMSEventMessage.REMOVE_ALL:
                removeAll(cache);
                break;
            default:
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.severe(" Undefined action " + event);
                }
        }
    }

    private void removeAll(Cache cache) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("removeAll ");
        }
        cache.removeAll(true);
    }

    private void remove(Cache cache, Element element) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("remove ( element = " + element + " ) ");
        }
        cache.remove(element, true);
    }

    private void put(Cache cache, Element element) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("put ( element = " + element + " ) ");
        }
        cache.put(element, true);
    }

    /**
     * Gets a list of elements from the cache, for a list of keys, without updating Element statistics. Time to
     * idle lifetimes are therefore not affected.
     * <p/>
     * Cache statistics are still updated.
     * @param keys a list of serializable values which represent keys
     * @return a list of Elements. If an element was not found or null, it will not be in the list.
     */
    public List getElements(List keys) throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("getElements ( keys = " + keys + " ) called ");
        }

        return null;
    }

    /**
     * Gets the globally unique id for the underlying <code>Cache</code> instance.
     * @return a String representation of the GUID
     * @throws RemoteException
     */
    public String getGuid() throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("getGuid ( ) called ");
        }
        return null;
    }

    /**
     * Returns a list of all elements in the cache, whether or not they are expired.
     * <p/>
     * The returned keys are unique and can be considered a set.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(n). On a single cpu 1.8Ghz P4, approximately 8ms is required
     * for each 1000 entries.
     *
     * @return a list of {@link Object} keys
     */
    public List getKeys() throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("getKeys ( ) called ");
        }

        return null;
    }

    /**
     * Gets the cache name.
     */
    public String getName() throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("getName ( ) called ");
        }

        return null;
    }

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated.
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     */
    public Element getQuiet(Serializable key) throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("getQuiet ( key = " + key + " ) called ");
        }

        return null;
    }

    /**
     * The URL for the remote replicator to connect. The value will only have meaning
     * for a specific implementation of replicator and remote peer.
     * <p/>
     * This method is not meant to be used remotely. The replicator already needs to know this. It has
     * to throw RemoteException to comply with RMI requirements
     * @return the URL as a string
     */
    public String getUrl() throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("getUrl ( ) called ");
        }
        return null;
    }

    /**
     * The URL base for the remote replicator to connect. The value will have meaning
     * only to a specific implementation of replicator and remote peer.
     */
    public String getUrlBase() throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("getUrlBase ( ) called ");
        }

        return null;
    }

    /**
     * todo
     * Put an element in the cache.
     * <p/>
     * Resets the access statistics on the element, which would be the case if it has previously been
     * gotten from a cache, and is now being put back.
     *
     * @param element
     * @throws IllegalStateException    if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public void put(Element element) throws IllegalArgumentException,
            IllegalStateException, RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("put ( element = " + element + " ) called ");
        }

    }

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache. This also removes it from any
     * stores it may be in.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public boolean remove(Serializable key) throws IllegalStateException,
            RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("remove ( key = " + key + " ) called ");
        }

        return false;
    }

    /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public void removeAll() throws RemoteException, IllegalStateException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("removeAll ( ) called ");
        }

    }

    /**
     * Send the cache peer with an ordered list of {@link net.sf.ehcache.distribution.EventMessage}s.
     * <p/>
     * This enables multiple messages to be delivered in one network invocation.
     * @param eventMessages a list of type {@link net.sf.ehcache.distribution.EventMessage}
     */
    public void send(List eventMessages) throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("send ( eventMessages = " + eventMessages + " ) called ");
        }

        for (Iterator iter = eventMessages.iterator(); iter.hasNext();) {
            try {
                ObjectMessage message = createMessage((JMSEventMessage) iter.next(), nodeName);
                messageProducer.send(message);
            } catch (JMSException e) {
                throw new RemoteException(e.getMessage());
            }
        }
    }

    /**
     * 
     * @param object
     * @param nodeName
     * @return
     * @throws JMSException
     */
    protected ObjectMessage createMessage(JMSEventMessage object, String nodeName) throws JMSException {
        object.setNodeName(nodeName);
        ObjectMessage message = producerSession.createObjectMessage();
        message.setObject(object);
        return message;
    }

    /**
     * 
     * @param message
     */
    public void onMessage(Message message) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("onMessage ( message = " + message + " ) called");
        }

        try {

            if (!(message instanceof ObjectMessage)) {
                LOG.severe("Cannot handle message of type (class="
                        + message.getClass().getName()
                        + "). Notification ignored.");
                return;
            }

            ObjectMessage objectMessage = (ObjectMessage) message;

            Object o = objectMessage.getObject();

            if (!(o instanceof JMSEventMessage)) {
                LOG.severe("Cannot handle message of type (class="
                        + o.getClass().getName() + "). Notification ignored.");
                return;
            }

            JMSEventMessage m = (JMSEventMessage) o;
            if (!nodeName.equals(m.getNodeName())) {
                handleNotification(m);
            } else {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Same nodeName, not handling this message.");
                }
            }

        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Cannot handle cluster Notification", e);
        }
    }
}
