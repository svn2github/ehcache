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
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CachePeer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * todo separate sending and receiving into separate classes
 * A JMS Cache Peer subscribes to JMS messages
 *
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSCachePeer implements CachePeer, MessageListener {

    private static final Logger LOG = Logger.getLogger(JMSCachePeer.class.getName());

    private Session producerSession;
    private CacheManager cacheManager;
    private MessageProducer messageProducer;

    /**
     * @param cacheManager
     * @param messageProducer
     * @param producerSession
     */
    public JMSCachePeer(CacheManager cacheManager, MessageProducer messageProducer, Session producerSession) {


        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("JMSCachePeer constructor ( cacheManager = "
                    + cacheManager
                    + ", messageProducer = " + messageProducer + " ) called");
        }

        this.cacheManager = cacheManager;
        this.messageProducer = messageProducer;
        this.producerSession = producerSession;

    }

    /**
     * Unwraps the JMSEventMessage and performs the cache action
     *
     * @param message
     * @param cache
     */
    private void handleNotification(JMSEventMessage message, Ehcache cache) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("handleNotification ( message = " + message + " ) called ");
        }

        int event = message.getEvent();

        switch (event) {
            case JMSEventMessage.PUT:
                put(cache, message.getElement());
                break;
            case JMSEventMessage.REMOVE:
                remove(cache, message.getSerializableKey());
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

    private void removeAll(Ehcache cache) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("removeAll ");
        }
        cache.removeAll(true);
    }

    private void remove(Ehcache cache, Serializable key) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("remove ( key = " + key + " ) ");
        }
        cache.remove(key, true);
    }

    private void put(Ehcache cache, Element element) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("put ( element = " + element + " ) ");
        }
        cache.put(element, true);
    }

    /**
     * Send the cache peer with an ordered list of {@link net.sf.ehcache.distribution.EventMessage}s.
     * <p/>
     * This enables multiple messages to be delivered in one network invocation.
     *
     * @param eventMessages a list of type {@link net.sf.ehcache.distribution.EventMessage}
     */
    public void send(List eventMessages) throws RemoteException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("send ( eventMessages = " + eventMessages + " ) called ");
        }

        for (Object eventMessage : eventMessages) {
            try {
                ObjectMessage message = producerSession.createObjectMessage((JMSEventMessage) eventMessage);
                messageProducer.send(message);
            } catch (JMSException e) {
                throw new RemoteException(e.getMessage());
            }
        }
    }

    /**
     * @param message
     */
    public void onMessage(Message message) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("onMessage ( message = " + message + " ) called");
        }

        try {
            //todo cater for other message types from non-cache senders
            if (!(message instanceof ObjectMessage)) {
                LOG.severe("Cannot handle message of type (class=" + message.getClass().getName()
                        + "). Notification ignored.");
                return;
            }

            ObjectMessage objectMessage = (ObjectMessage) message;
            Object object = objectMessage.getObject();

            if (!(object instanceof JMSEventMessage)) {
                LOG.severe("Cannot handle message of type (class="
                        + object.getClass().getName() + "). Notification ignored.");
                return;
            }
            JMSEventMessage jmsEventMessage = (JMSEventMessage) object;

            Cache cache;
            String cacheName = null;
            try {
                cacheName = jmsEventMessage.getCacheName();
                cache = cacheManager.getCache(cacheName);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
                return;
            }
            handleNotification(jmsEventMessage, cache);

        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Exception handling JMS Notification", e);
        }
    }

    /**
     * Not implemented for JMS
     *
     * @param keys a list of serializable values which represent keys
     * @return a list of Elements. If an element was not found or null, it will not be in the list.
     */
    public List getElements(List keys) throws RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * Not implemented for JMS
     *
     * @return a String representation of the GUID
     * @throws RemoteException
     */
    public String getGuid() throws RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * Not implemented for JMS
     *
     * @return a list of {@link Object} keys
     */
    public List getKeys() throws RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * Not implemented for JMS
     */
    public String getName() throws RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * Not implemented for JMS
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     */
    public Element getQuiet(Serializable key) throws RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * Not implemented for JMS
     *
     * @return the URL as a string
     */
    public String getUrl() throws RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * The URL base for the remote replicator to connect. The value will have meaning
     * only to a specific implementation of replicator and remote peer.
     */
    public String getUrlBase() throws RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * Not implemented for JMS
     *
     * @param element the element to put
     * @throws IllegalStateException    if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * Not implemented for JMS
     *
     * @param key the element key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public boolean remove(Serializable key) throws IllegalStateException, RemoteException {
        throw new RemoteException("Not implemented for JMS");
    }

    /**
     * Not implemented for JMS
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public void removeAll() throws RemoteException, IllegalStateException {
        throw new RemoteException("Not implemented for JMS");
    }


}
