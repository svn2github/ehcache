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


package net.sf.ehcache.distribution.jgroups;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.management.ManagedCacheManagerPeerProvider;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.blocks.NotificationBus;
import org.jgroups.jmx.JmxConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main Jgroup class for replication via JGroup. Starts up the Jgroup communication bus and listen for message in
 * the bus. Because of Ehcache design we have to register this as a CachePeer. In reality this class listen for change
 * on the bus and tells the cachemanager to update.
 *
 * @author Pierre Monestie (pmonestie__REMOVE__THIS__@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class JGroupManager implements NotificationBus.Consumer, CachePeer, ManagedCacheManagerPeerProvider {

    private static final String JMX_DOMAIN_NAME = "JGroupsReplication";

    private static final Logger LOG = LoggerFactory.getLogger(JGroupManager.class.getName());

    private static final int CHUNK_SIZE = 100;

    private NotificationBus notificationBus;
    private MBeanServer mBeanServer;

    private CacheManager cacheManager;

    /**
     * Construct a new JGroupManager with a specific JGroups connection String
     *
     * @param cacheManager the cache manager
     * @param connect      the connection String
     */
    public JGroupManager(CacheManager cacheManager, String connect) {

        try {
            this.cacheManager = cacheManager;
            notificationBus = new NotificationBus("EH_CACHE", connect);
            notificationBus.start();
            notificationBus.getChannel().setOpt(Channel.LOCAL, Boolean.FALSE);
            notificationBus.setConsumer(this);
            LOG.info("JGroupManager started. address is " + this.notificationBus.getLocalAddress());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

    }

    /**
     * {@inheritDoc}
     */
    public Serializable getCache() {
        return null;
    }

    private void handleJGroupNotification(JGroupSerializable e) {
        Cache cache = cacheManager.getCache(e.getCacheName());
        if (cache != null) {
            if (e.getEvent() == JGroupEventMessage.REMOVE && cache.getQuiet(e.getKey()) != null) {
                LOG.debug("received remove:          cache={}, key={}", e.getCacheName(), e.getKey());
                cache.remove(e.getKey(), true);
            } else if (e.getEvent() == JGroupEventMessage.PUT) {
                LOG.debug("received put:             cache={}, key={}", e.getCacheName(), e.getKey());
                cache.put(new Element(e.getKey(), e.getValue()), true);
            } else if (e.getEvent() == JGroupEventMessage.BOOTSTRAP_REPLY) {
                LOG.debug("received bootstrap reply: cache={}, key={}", e.getCacheName(), e.getKey());
                cache.put(new Element(e.getKey(), e.getValue()), true);
            } else if (e.getEvent() == JGroupEventMessage.REMOVE_ALL) {
                LOG.debug("remove all");
                cache.removeAll(true);
            } else if (e.getEvent() == JGroupEventMessage.ASK_FOR_BOOTSTRAP) {
                sendBootstrapResponse(e, cache);
            }
        }

    }

    private void sendBootstrapResponse(JGroupSerializable e, Cache cache) {
        Address requestAddress = (Address) e.getKey();
        LOG.debug("received bootstrap request from {}, cache={}", requestAddress, e.getCacheName());
        List keys = cache.getKeys();
        if (keys != null && keys.size() > 0) {

            List<JGroupEventMessage> messageList = new ArrayList<JGroupEventMessage>();
            for (Object key : keys) {
                Element element = cache.get(key);
                JGroupEventMessage jGroupEventMessage =
                        new JGroupEventMessage(JGroupEventMessage.BOOTSTRAP_REPLY,
                                (Serializable) key, element, cache, cache.getName());
                messageList.add(jGroupEventMessage);

                if (messageList.size() == CHUNK_SIZE) {
                    sendResponseChunk(cache, requestAddress, messageList);
                    messageList = new ArrayList<JGroupEventMessage>();
                }

            }
            //send remainders
            if (messageList.size() > 0) {
                sendResponseChunk(cache, requestAddress, messageList);
            }

        } else {
            LOG.debug("no keys to reply to {} to boot cache {}", requestAddress, cache.getName());
        }
    }

    private void sendResponseChunk(Cache cache, Address requestAddress, List events) {
        LOG.debug("reply {} elements to {} to boot cache {}", new Object[] {events.size(), requestAddress, cache.getName()});
        try {
            send(requestAddress, events);
        } catch (RemoteException e1) {
            LOG.error("error repling to {}", requestAddress, e1);
        }
    }

    /**
     * Handles notification: Looks at type of message and unwrap if the argument is a list
     */
    public void handleNotification(Serializable arg0) {

        if (arg0 instanceof JGroupSerializable) {

            handleJGroupNotification((JGroupSerializable) arg0);
        } else if (arg0 instanceof List) {

            List l = (List) arg0;

            for (int i = 0; i < l.size(); i++) {
                Object obj = l.get(i);
                if (obj instanceof JGroupSerializable) {
                    handleJGroupNotification((JGroupSerializable) obj);
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public void memberJoined(Address arg0) {
        LOG.debug("joined: {}", arg0);

    }

    /**
     * {@inheritDoc}
     */
    public void memberLeft(Address arg0) {
        LOG.debug("left: {}", arg0);

    }

    /**
     * {@inheritDoc}
     */
    public List getElements(List keys) throws RemoteException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getGuid() throws RemoteException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() throws RemoteException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() throws RemoteException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Serializable key) throws RemoteException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getUrl() throws RemoteException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getUrlBase() throws RemoteException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, RemoteException {

    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Serializable key) throws IllegalStateException, RemoteException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws RemoteException, IllegalStateException {

    }

    private JGroupSerializable wrapMessage(JGroupEventMessage msg) {
        Serializable value = (msg.getElement() == null ? null : msg.getElement().getValue());
        return new JGroupSerializable(msg.getEvent(), msg.getSerializableKey(), value, msg.getCacheName());
    }

    /**
     * {@inheritDoc}
     */
    public void send(List eventMessages) throws RemoteException {
        send(null, eventMessages);
    }

    /**
     * Sends a message to a single address
     */
    public void send(Address address, List eventMessages) throws RemoteException {
        if (eventMessages.size() == 1) {
            notificationBus.sendNotification(wrapMessage((JGroupEventMessage) eventMessages.get(0)));
            return;
        }
        ArrayList<JGroupSerializable> msg = new ArrayList<JGroupSerializable>();

        for (Iterator iter = eventMessages.iterator(); iter.hasNext();) {
            JGroupEventMessage m = (JGroupEventMessage) iter.next();
            msg.add(wrapMessage(m));
        }

        try {

            notificationBus.sendNotification(address, msg);
        } catch (Throwable t) {
            throw new RemoteException(t.getMessage());
        }

    }

    /**
     * @return the {@link Status} of the manager
     */
    public Status getStatus() {
        if (notificationBus == null) {
            return Status.STATUS_UNINITIALISED;
        }
        if (notificationBus.getChannel() == null) {
            return Status.STATUS_SHUTDOWN;
        }

        return Status.STATUS_ALIVE;
    }
    
    /**
     * @return The cluster name for JMX registration 
     */
    protected String getJmxClusterName() {
        return this.cacheManager.getName();
    }
    
    /**
     * {@inheritDoc}
     */
    public void register(MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
        
        final Channel channel = this.notificationBus.getChannel();
        try {
            JmxConfigurator.registerChannel((JChannel)channel, mBeanServer, JMX_DOMAIN_NAME, this.getJmxClusterName(), true);
        } catch (Exception e) {
            LOG.error("Error occured while registering MBeans:", e);
        }        
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() throws CacheException {
        if (notificationBus != null) {
            if (this.mBeanServer != null) {
                final Channel channel = this.notificationBus.getChannel();
                try {
                    JmxConfigurator.unregisterChannel((JChannel)channel, mBeanServer, JMX_DOMAIN_NAME, this.getJmxClusterName());
                } catch (Exception e) {
                    LOG.error("Error occured while unregistering MBeans:", e);
                }
            }
            
            try {
                notificationBus.stop();
            } catch (Exception e) {
                LOG.error("Error occured while closing Manager:", e);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public long getTimeForClusterToForm() {
        return 0;
    }

    /**
     * The replication scheme. Each peer provider has a scheme name, which can be used to specify the scheme for
     * replication and bootstrap purposes. Each <code>CacheReplicator</code> should lookup the provider for its scheme
     * type during replication. Similarly a <code>BootstrapCacheLoader</code> should also look up the provider for its
     * scheme.
     * <p/>
     *
     * @return the well-known scheme name, which is determined by the replication provider author.
     * @since 1.6 introduced to permit multiple distribution schemes to be used in the same CacheManager
     */
    public String getScheme() {
        return "JGroups";
    }

    /**
     * {@inheritDoc}
     */
    public void init() {

    }

    /**
     * {@inheritDoc}
     * We act as our own peer in this implementation
     */
    public List listRemoteCachePeers(Ehcache cache) throws CacheException {
        ArrayList<JGroupManager> a = new ArrayList<JGroupManager>();
        a.add(this);
        return a;
    }

    /**
     * {@inheritDoc}
     */
    public void registerPeer(String rmiUrl) {

    }

    /**
     * {@inheritDoc}
     */
    public void unregisterPeer(String rmiUrl) {

    }

    /**
     * @return
     */
    public List getBusMembership() {
        return notificationBus.getMembership();
    }

    /**
     * @return
     */
    public Address getBusLocalAddress() {
        return notificationBus.getLocalAddress();
    }

}
