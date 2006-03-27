/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.distribution.CacheReplicator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Registered listeners for registering and unregistering CacheEventListeners and multicasting notifications to registrants.
 * <p/>
 * There is one of these per Cache
 *
 * @author Greg Luck
 * @version $Id: RegisteredEventListeners.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class RegisteredEventListeners {

    /**
     * A Map of CacheEventListeners keyed by listener class.
     * CacheEventListener implementations that will be notified of this cache's events.
     *
     * @see CacheEventListener
     */
    private Set cacheEventListeners = new HashSet();
    private Cache cache;

    /**
     * Constructs a new notification service
     *
     * @param cache
     */
    public RegisteredEventListeners(Cache cache) {
        this.cache = cache;
    }


    /**
     * Notifies all registered listeners, in no guaranteed order, that an element was removed
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementRemoved
     */
    public void notifyElementRemoved(Element element, boolean remoteEvent) throws CacheException {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                cacheEventListener.notifyElementRemoved(cache, element);
            }
        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element was put into the cache
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementPut(net.sf.ehcache.Cache,net.sf.ehcache.Element)
     */
    public void notifyElementPut(Element element, boolean remoteEvent) throws CacheException {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                cacheEventListener.notifyElementPut(cache, element);
            }
        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element in the cache was updated
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementPut(net.sf.ehcache.Cache,net.sf.ehcache.Element)
     */
    public void notifyElementUpdated(Element element, boolean remoteEvent) {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                cacheEventListener.notifyElementUpdated(cache, element);
            }
        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element has expired
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementExpired
     */
    public void notifyElementExpiry(Element element, boolean remoteEvent) {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                cacheEventListener.notifyElementExpired(cache, element);
            }
        }
    }


    /**
     * CacheReplicators should not be notified of events received remotely, as this would cause
     * a circular notification
     *
     * @param remoteEvent
     * @param cacheEventListener
     * @return true is notifiying the listener would cause a circular notification
     */
    private boolean isCircularNotification(boolean remoteEvent, CacheEventListener cacheEventListener) {
        return remoteEvent && cacheEventListener instanceof CacheReplicator;
    }


    /**
     * Adds a listener to the notification service. No guarantee is made that listeners will be
     * notified in the order they were added.
     *
     * @param cacheEventListener
     * @return true if the listener is being added and was not already added
     */
    public boolean registerListener(CacheEventListener cacheEventListener) {
        if (cacheEventListener == null) {
            return false;
        }
        return cacheEventListeners.add(cacheEventListener);
    }

    /**
     * Removes a listener from the notification service.
     *
     * @param cacheEventListener
     * @return true if the listener was present
     */
    public boolean unregisterListener(CacheEventListener cacheEventListener) {
        return cacheEventListeners.remove(cacheEventListener);
    }

    /**
     * Gets a list of the listeners registered to this class
     *
     * @return a list of type <code>CacheEventListener</code>
     */
    public Set getCacheEventListeners() {
        return cacheEventListeners;
    }

    /**
     * Tell listeners to dispose themselves.
     * Because this method is only ever called from a synchronized cache method, it does not itself need to be
     * synchronized.
     */
    public void dispose() {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            cacheEventListener.dispose();
        }

        cacheEventListeners.clear();
    }
}
