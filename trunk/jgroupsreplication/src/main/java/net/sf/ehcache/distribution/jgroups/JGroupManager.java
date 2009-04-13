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

package net.sf.ehcache.distribution.jgroups;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.blocks.NotificationBus;
import org.jgroups.stack.IpAddress;

/**
 * The main Jgroup class for replication via JGroup. Starts up the Jgroup communication bus and listen for message in
 * the bus. Because of Ehcache design we have to register this as a CachePeer. In reality this class listen for change
 * on the bus and tells the cachemanager to update.
 *
 * @author Pierre Monestie (pmonestie__REMOVE__THIS__@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class JGroupManager implements NotificationBus.Consumer, CachePeer, CacheManagerPeerProvider {

    private static final Logger LOG = Logger.getLogger(JGroupManager.class.getName());

    private NotificationBus bus;

    private CacheManager cacheManager;

    /**
     * Construct a new JGroupManager with a specific JGroups connection String
     *
     * @param m the cache manager
     * @param connect the connection String
     */
    public JGroupManager(CacheManager m, String connect) {

        try {
            this.cacheManager = m;
            this.bus = new NotificationBus("EH_CACHE", connect);

            // manager.bus = new NotificationBus(busName,
            // DEFAULT_CHANNEL_PROPERTIES_POST);

            this.bus.start();
            this.bus.getChannel().setOpt(Channel.LOCAL, Boolean.FALSE);
            this.bus.setConsumer(this);
            LOG.info("GMS started. address is " + this.bus.getLocalAddress());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
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
            if (e.getEvent() == e.REMOVE && cache.getQuiet(e.getKey()) != null) {
                cache.remove(e.getKey(), true);
            } else if (e.getEvent() == e.PUT) {
                cache.put(new Element(e.getKey(), e.getValue()), true);
            } else if (e.getEvent() == e.BOOTSTRAP_REPLY) {
                LOG.fine("received bootstrap reply: cache=" + e.getCacheName() + ", key=" + e.getKey());
                cache.put(new Element(e.getKey(), e.getValue()), true);
            } else if (e.getEvent() == e.REMOVE_ALL) {
                LOG.info("remove all");
                cache.removeAll(true);
            } else if (e.getEvent() == e.ASK_FOR_BOOTSTRAP) {
                IpAddress requestAddress = (IpAddress) e.getKey();
                LOG.info("received bootstrap request from " + requestAddress + ", cache=" + e.getCacheName());
                List keys = cache.getKeys();
                if (keys != null && keys.size() > 0) {

                    List events = new ArrayList();
                    for (Object key : keys) {
                        Element element = cache.get(key);
                        JGroupEventMessage r = new JGroupEventMessage(JGroupEventMessage.BOOTSTRAP_REPLY, (Serializable) key, element, cache, cache.getName());
                        events.add(r);
                        if (events.size() > 99) {
                            LOG.info("reply " + events.size() + " elements to " + requestAddress + " to boot cache " + cache.getName());
                            try {
                                send(requestAddress, events);
                            } catch (RemoteException e1) {
                                LOG.log(Level.SEVERE, "error repling to " + requestAddress, e1);
                            }
                            events = new ArrayList();
                        }
                    }
                    if (events.size() > 0) {
                        LOG.info("reply " + events.size() + " elements to " + requestAddress + " to boot cache " + cache.getName());
                        try {
                            send(requestAddress, events);
                        } catch (RemoteException e1) {
                            LOG.log(Level.SEVERE, "error repling to " + requestAddress, e1);
                        }
                    }

                } else {
                    LOG.log(Level.FINE, "no keys to reply to " + requestAddress + " to boot cache " + cache.getName());
                }
            }
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
        LOG.info("joined:" + arg0);

    }

    /**
     * {@inheritDoc}
     */
    public void memberLeft(Address arg0) {
        LOG.info("left:" + arg0);

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
        Object value = (msg.getElement() == null ? null : msg.getElement().getValue());
        return new JGroupSerializable(msg.getEvent(), msg.getSerializableKey(), value, msg.getCacheName());
    }

    /**
     * {@inheritDoc}
     */
    public void send(List eventMessages) throws RemoteException {
        send(null, eventMessages);
    }

    public void send(Address address, List eventMessages) throws RemoteException {
        if (eventMessages.size() == 1) {
            bus.sendNotification(wrapMessage((JGroupEventMessage) eventMessages.get(0)));
            return;
        }
        ArrayList msg = new ArrayList();

        for (Iterator iter = eventMessages.iterator(); iter.hasNext();) {
            JGroupEventMessage m = (JGroupEventMessage) iter.next();
            msg.add(wrapMessage(m));
        }

        try {

            bus.sendNotification(address, msg);
        } catch (Throwable t) {
            throw new RemoteException(t.getMessage());
        }

    }

    /**
     * @return the {@link Status} of the manager
     */
    public Status getStatus() {
        if (bus == null) {
            return Status.STATUS_UNINITIALISED;
        }
        if (bus.getChannel() == null) {
            return Status.STATUS_SHUTDOWN;
        }

        return Status.STATUS_ALIVE;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() throws CacheException {
        if (bus != null) {
            try {
                bus.stop();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error occured while closing Manager:", e);
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
        ArrayList a = new ArrayList();
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

    public Vector getBusMembership() {
        return bus.getMembership();
    }

    public Address getBusLocalAddress() {
        return bus.getLocalAddress();
    }

}
