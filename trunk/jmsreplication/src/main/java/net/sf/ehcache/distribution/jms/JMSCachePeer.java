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
import net.sf.ehcache.MimeTypeByteArray;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.distribution.CachePeer;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.CACHE_NAME_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.Action;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.ACTION_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.KEY_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSUtil.CACHE_MANAGER_UID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.BytesMessage;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.ConnectionConsumer;
import javax.jms.QueueReceiver;
import javax.jms.DeliveryMode;
import javax.jms.Queue;
import javax.jms.Destination;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A JMS Cache Peer subscribes to JMS messages, both from the replication topic and the get queue.
 *
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSCachePeer implements CachePeer, MessageListener {

    private static final Logger LOG = Logger.getLogger(JMSCachePeer.class.getName());

    private Session producerSession;
    private CacheManager cacheManager;
    private MessageProducer messageProducer;
    private QueueSession getQueueSession;


    /**
     * Constructor
     */
    public JMSCachePeer(CacheManager cacheManager,
                        MessageProducer messageProducer,
                        Session producerSession,
                        QueueSession getQueueSession) {


        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("JMSCachePeer constructor ( cacheManager = "
                    + cacheManager
                    + ", messageProducer = " + messageProducer + " ) called");
        }

        this.cacheManager = cacheManager;
        this.messageProducer = messageProducer;
        this.producerSession = producerSession;
        this.getQueueSession = getQueueSession;
    }

    /**
     * Process a cache replication message.
     * <p/>
     * Unwraps the JMSEventMessage and performs the cache action
     * <p/>
     *
     * @param message the message, which contains a payload and action
     * @param cache   the cache to perform the action upon
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



    /**
     * Process a non-cache message
     * <p/>
     * Performs the cache action
     *
     * @param element the element which was sent over JMS in an ObjectMessage
     * @param cache   the cache to perform the action upon
     * @param action  the action to perform
     */
    private void handleNotification(Element element, Serializable key, Ehcache cache, Action action) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("handleNotification ( element = " + element + " ) called ");
        }

        if (action.equals(Action.PUT)) {
            put(cache, element);
        } else if (action.equals(Action.REMOVE)) {
            remove(cache, key);
        } else if (action.equals(Action.REMOVE_ALL)) {
            removeAll(cache);
        }
    }

    /**
     * Process a non-cache message
     * <p/>
     * Performs the cache action
     *
     * @param cache  the cache to perform the action upon
     * @param action the action to perform
     */
    private void handleNotification(Object object, Serializable key, Ehcache cache, Action action) {

        Element element = new Element(key, object);
        if (action.equals(Action.PUT)) {
            put(cache, element);
        } else if (action.equals(Action.REMOVE)) {
            remove(cache, key);
        } else if (action.equals(Action.REMOVE_ALL)) {
            removeAll(cache);
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
                LOG.log(Level.SEVERE, e.getMessage(), e);
                throw new RemoteException(e.getMessage());
            }
        }
    }

    /**
     * @param message a JMSMessage guaranteed to not be sent to the publishing CacheManager instance.
     */
    public void onMessage(Message message) {

        try {
            if (message instanceof ObjectMessage) {

                ObjectMessage objectMessage = (ObjectMessage) message;
                Object object = objectMessage.getObject();


                //If a non-cache publisher sends an Element
                if (object instanceof Element) {
                    LOG.fine(getName() +  ": Element message received - " + object);


                    Element element = (Element) object;

                    Cache cache = extractAndValidateCache(objectMessage);
                    Action action = extractAndValidateAction(objectMessage);
                    //not required for Element
                    Serializable key = extractAndValidateKey(objectMessage, action);
                    handleNotification(element, key, cache, action);

                } else if (object instanceof JMSEventMessage) {
                    LOG.fine(getName() +  ": JMSEventMessage message received - " + object);


                    //no need for cacheName, mimeType, key or action properties as all are in message.
                    JMSEventMessage jmsEventMessage = (JMSEventMessage) object;
                    LOG.info("" + jmsEventMessage);

                    Cache cache;
                    String cacheName = null;
                    try {
                        cacheName = jmsEventMessage.getCacheName();
                        cache = cacheManager.getCache(cacheName);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                        return;
                    }
                    if (jmsEventMessage.getEvent() == JMSEventMessage.Action.GET.toInt()) {
                        handleGetRequest(objectMessage, jmsEventMessage, cache);
                    } else {
                        handleNotification(jmsEventMessage, cache);
                    }

                } else {
                    LOG.fine(getName() +  ": Other ObjectMessage received - " + object);


                    //no need for mimeType. An object has a type
                    Cache cache = extractAndValidateCache(objectMessage);
                    Action action = extractAndValidateAction(objectMessage);
                    Serializable key = extractAndValidateKey(objectMessage, action);
                    handleNotification(object, key, cache, action);
                }
            } else if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                LOG.fine(getName() +  ": Other ObjectMessage received - " + textMessage);

                Cache cache = extractAndValidateCache(message);
                Action action = extractAndValidateAction(message);
                Serializable key = extractAndValidateKey(message, action);
                String mimeType = extractAndValidateMimeType(message, action);
                byte[] payload = new byte[0];
                if (textMessage.getText() != null) {
                    payload = textMessage.getText().getBytes();
                }
                MimeTypeByteArray value = new MimeTypeByteArray(mimeType, payload);
                handleNotification(value, key, cache, action);

            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                LOG.fine(getName() +  ": Other ObjectMessage received - " + bytesMessage);

                Cache cache = extractAndValidateCache(message);
                Action action = extractAndValidateAction(message);
                Serializable key = extractAndValidateKey(message, action);
                String mimeType = extractAndValidateMimeType(message, action);
                byte[] payload = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(payload);
                MimeTypeByteArray value = new MimeTypeByteArray(mimeType, payload);
                handleNotification(value, key, cache, action);
            } else {
                throw new InvalidJMSMessageException("Cannot handle message of type (class=" + message.getClass().getName()
                        + "). Notification ignored.");
            }

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to handle JMS Notification: " + e.getMessage(), e);
        }
    }

    private void handleGetRequest(ObjectMessage objectMessage, JMSEventMessage jmsEventMessage, Cache cache) throws JMSException {
        LOG.info(cacheManager.getName() +  ": JMSEventMessage message received - " + objectMessage.getJMSMessageID());
        Serializable key = jmsEventMessage.getSerializableKey();
        Element element = cache.get(key);
        Serializable value = null;
        if (element != null) {
            value = element.getValue();
        }
        assert(objectMessage.getIntProperty(CACHE_MANAGER_UID) != JMSUtil.localCacheManagerUid(cache)) :
                "The JMSCachePeer received a getQueue request sent by a JMSCacheLoader belonging to the same" +
                        "CacheManager, which is invalid";
        ObjectMessage reply = getQueueSession.createObjectMessage(value);
        String name = null;
        try {
            name = getName();
        } catch (RemoteException e) {
            //impossible - local call
        }
        reply.setStringProperty("responder", name);
        reply.setJMSCorrelationID(objectMessage.getJMSMessageID());
        Queue replyQueue = (Queue) objectMessage.getJMSReplyTo();

        QueueSender replyQueueSender = getQueueSession.createSender(replyQueue);
        replyQueueSender.send(reply);
    }

    private Serializable extractAndValidateKey(Message message, Action action) throws JMSException {
        String key = message.getStringProperty(KEY_PROPERTY);
        if (key == null && action.equals(Action.REMOVE)) {
            throw new InvalidJMSMessageException("No key property specified. The key is required when the action is REMOVE.");
        }
        return key;
    }

    private String extractAndValidateMimeType(Message message, Action action) throws JMSException {
        String mimeType = message.getStringProperty(JMSEventMessage.MIME_TYPE_PROPERTY);
        if (mimeType == null && action.equals(Action.PUT)) {
            if (message instanceof TextMessage) {
                mimeType = "text/plain";
            } else if (message instanceof BytesMessage) {
                mimeType = "application/octet-stream";
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("mimeType property not set. Auto setting MIME Type for message " + message.getJMSMessageID() + " to " + mimeType);
            }
        }
        return mimeType;
    }

    private Action extractAndValidateAction(Message message) throws JMSException {
        String actionString = message.getStringProperty(ACTION_PROPERTY);
        Action action;
        if (actionString == null || (action = Action.valueOf(actionString)) == null) {
            throw new InvalidJMSMessageException("No action specified. Must be one of PUT, REMOVE or REMOVE_ALL");
        }
        return action;
    }

    private Cache extractAndValidateCache(Message message) throws JMSException {
        Cache cache;
        String cacheName = message.getStringProperty(CACHE_NAME_PROPERTY);
        if (cacheName == null) {
            throw new InvalidJMSMessageException("No cache name specified.");
        }
        cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new InvalidJMSMessageException("No cache named " + cacheName + "exists in the target CacheManager.");
        }
        return cache;
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
        return cacheManager.getName() + " JMSCachePeer";
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
