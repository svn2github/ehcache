/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.blocks.NotificationBus;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author Pierre Monestie (pmonestie__REMOVE__THIS__@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class JGroupManager implements NotificationBus.Consumer, CachePeer, CacheManagerPeerProvider {

    private static String hostname = "localhost";

    private static final Log log = LogFactory.getLog(JGroupManager.class);

    private NotificationBus bus;

    CacheManager cacheManager;

    static HashMap properties = new HashMap();

    public JGroupManager(CacheManager m, String connect) {

        try {
            this.cacheManager = m;
            this.bus = new NotificationBus("EH_CACHE", connect);

            // manager.bus = new NotificationBus(busName,
            // DEFAULT_CHANNEL_PROPERTIES_POST);

            this.bus.start();
            this.bus.getChannel().setOpt(Channel.LOCAL, new Boolean(false));
            this.bus.setConsumer(this);
            log.info("GMS started. address is " + this.bus.getLocalAddress());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    public Serializable getCache() {

        // TODO Auto-generated method stub
        return null;
    }

    private void handleJGroupNotification(JGroupSerializable e) {
        // log.debug("got notified:"+e.getSerializableKey());
        Cache c = cacheManager.getCache(e.getCacheName());
        if (c != null) {
            if (e.getEvent() == e.REMOVE && c.getQuiet(e.getKey()) != null) {

                c.remove(e.getKey(), true);
            } else if (e.getEvent() == e.PUT) {

                c.put(new Element(e.getKey(), e.getValue()), true);
            } else if (e.getEvent() == e.REMOVE_ALL) {
                // c.removeAll(true);
                log.debug("remove all");
                c.removeAll(true);
            }
        }

    }

    /**
     * Handles notification: Looks at type of message and unwrap if the argument
     * is a list
     */
    public void handleNotification(Serializable arg0) {

        if (arg0 instanceof JGroupSerializable) {


            handleJGroupNotification((JGroupSerializable) arg0);
        } else if (arg0 instanceof List) {

            List l = (List) arg0;

            for (int i = 0; i < l.size(); i++) {
                Object obj = l.get(i);
                if (obj instanceof JGroupSerializable)
                    handleJGroupNotification((JGroupSerializable) obj);
            }
        }

        // TODO Auto-generated method stub

    }

    public void memberJoined(Address arg0) {
        log.trace("joined:" + arg0);

    }

    public void memberLeft(Address arg0) {
        log.trace("left:" + arg0);

    }

    public List getElements(List keys) throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getGuid() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public List getKeys() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public Element getQuiet(Serializable key) throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUrl() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUrlBase() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public void put(Element element) throws IllegalArgumentException, IllegalStateException, RemoteException {
        // TODO Auto-generated method stub

    }

    public boolean remove(Serializable key) throws IllegalStateException, RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    public void removeAll() throws RemoteException, IllegalStateException {
        // TODO Auto-generated method stub

    }

    private JGroupSerializable wrapMessage(JGroupEventMessage msg) {
        Object value = (msg.getElement() == null ? null : msg.getElement().getValue());
        return new JGroupSerializable(msg.getEvent(), msg.getSerializableKey(), value, msg.getCacheName());
    }

    public void send(List eventMessages) throws RemoteException {
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

            bus.sendNotification(msg);
        } catch (Throwable t) {
            throw new RemoteException(t.getMessage());
        }

    }

    public Status getStatus() {
        if (bus == null)
            return Status.STATUS_UNINITIALISED;
        if (bus.getChannel() == null)
            return Status.STATUS_SHUTDOWN;

        return Status.STATUS_ALIVE;
    }

    public void dispose() throws CacheException {
        if (bus != null) {
            try {
                bus.stop();

            } catch (Exception e) {
            }
        }

    }

    public long getTimeForClusterToForm() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void init() {
        // TODO Auto-generated method stub

    }

    public List listRemoteCachePeers(Ehcache cache) throws CacheException {
        ArrayList a = new ArrayList();
        a.add(this);
        return a;
    }

    public void registerPeer(String rmiUrl) {
        // TODO Auto-generated method stub

    }

    public void unregisterPeer(String rmiUrl) {
        // TODO Auto-generated method stub

    }

}
