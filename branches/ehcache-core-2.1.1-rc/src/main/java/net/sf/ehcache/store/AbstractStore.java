/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.store;

import javax.swing.event.EventListenerList;

/**
 * @author gkeim
 * 
 */
public abstract class AbstractStore implements Store {
    /**
     * listener list
     */
    private transient EventListenerList listenerList;

    /**
     * onLoad initializer
     */
    protected synchronized EventListenerList getEventListenerList() {
        if (listenerList == null) {
            listenerList = new EventListenerList();
        }
        return listenerList;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.store.Store#isCacheCoherent()
     */
    public boolean isCacheCoherent() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.store.Store#isClusterCoherent()
     */
    public boolean isClusterCoherent() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.store.Store#isNodeCoherent()
     */
    public boolean isNodeCoherent() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.store.Store#setNodeCoherent(boolean)
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.store.Store#waitUntilClusterCoherent()
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.store.Store#addStoreListener(net.sf.ehcache.store.StoreListener)
     */
    public synchronized void addStoreListener(StoreListener listener) {
        removeStoreListener(listener);
        getEventListenerList().add(StoreListener.class, listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.store.Store#removeStoreListener(net.sf.ehcache.store.StoreListener)
     */
    public synchronized void removeStoreListener(StoreListener listener) {
        getEventListenerList().remove(StoreListener.class, listener);
    }

    /**
     * Message to StoreListeners the node's coherence state.
     */
    public void fireNodeCoherent(final boolean nodeCoherent) {
        Object[] listeners = getEventListenerList().getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == StoreListener.class) {
                ((StoreListener) listeners[i + 1]).nodeCoherent(nodeCoherent);
            }
        }
    }

    /**
     * Message to StoreListeners the cluster's coherence state.
     */
    public void fireClusterCoherent(final boolean clusterCoherent) {
        Object[] listeners = getEventListenerList().getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == StoreListener.class) {
                ((StoreListener) listeners[i + 1]).clusterCoherent(clusterCoherent);
            }
        }
    }
}
