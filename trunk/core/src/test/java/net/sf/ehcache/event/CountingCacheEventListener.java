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

package net.sf.ehcache.event;

import java.io.PrintWriter;
import java.io.StringWriter;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counts listener notifications.
 * <p/>
 * The methods also check that we hold the Cache lock.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CountingCacheEventListener implements CacheEventListener {

    private final List<CacheEvent> elementsPut = Collections.synchronizedList(new ArrayList<CacheEvent>());
    private final List<CacheEvent> elementsUpdated = Collections.synchronizedList(new ArrayList<CacheEvent>());
    private final List<CacheEvent> elementsRemoved = Collections.synchronizedList(new ArrayList<CacheEvent>());
    private final List<CacheEvent> elementsExpired = Collections.synchronizedList(new ArrayList<CacheEvent>());
    private final List<CacheEvent> elementsEvicted = Collections.synchronizedList(new ArrayList<CacheEvent>());
    private final List<CacheEvent> removeAlls = Collections.synchronizedList(new ArrayList<CacheEvent>());

    public List<CacheEvent> getCacheElementsRemoved() {
        return new ArrayList<CacheEvent>(elementsRemoved);
    }

    public List<CacheEvent> getCacheElementsPut() {
        return new ArrayList<CacheEvent>(elementsPut);
    }

    public List<CacheEvent> getCacheElementsUpdated() {
        return new ArrayList<CacheEvent>(elementsUpdated);
    }

    public List<CacheEvent> getCacheElementsExpired() {
        return new ArrayList<CacheEvent>(elementsExpired);
    }

    public List<CacheEvent> getCacheElementsEvicted() {
        return new ArrayList<CacheEvent>(elementsEvicted);
    }

    public List<CacheEvent> getCacheRemoveAlls() {
        return new ArrayList<CacheEvent>(removeAlls);
    }

    /**
     * Resets the counters to 0
     */
    public void resetCounters() {
        elementsRemoved.clear();
        elementsPut.clear();
        elementsUpdated.clear();
        elementsExpired.clear();
        elementsEvicted.clear();
        removeAlls.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(final Ehcache cache, final Element element) {
        elementsRemoved.add(new CacheEvent(element));
    }

    /**
     * Called immediately after an element has been put into the cache. The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
     * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
     *
     * @param cache
     * @param element the element which was just put into the cache.
     */
    public void notifyElementPut(final Ehcache cache, final Element element) {
        elementsPut.add(new CacheEvent(element));
    }


    /**
     * Called immediately after an element has been put into the cache and the element already
     * existed in the cache. This is thus an update.
     * <p/>
     * The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
     * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    public void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
        elementsUpdated.add(new CacheEvent(element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(final Ehcache cache, final Element element) {
        elementsExpired.add(new CacheEvent(element));
    }


    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(final Ehcache cache, final Element element) {
        elementsEvicted.add(new CacheEvent(element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(final Ehcache cache) {
        removeAlls.add(new CacheEvent(null));
    }

    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     * <p/>
     * Clean up static counters
     */
    public void dispose() {
        resetCounters();
    }

    /**
     * A Counter entry
     */
    public static class CacheEvent {

        private Element element;
        private Throwable stack;
        
        /**
         * Construct a new event
         *
         * @param cache
         * @param element
         */
        public CacheEvent(Element element) {
            this.element = element;
            this.stack = new Throwable();
        }

        /**
         * @return the payload
         */
        public Element getElement() {
            return element;
        }

        public Throwable getStack() {
            return stack;
        }
        
        @Override
        public String toString() {
          StringBuilder sb = new StringBuilder(element.toString()).append("\n");
          StringWriter writer = new StringWriter();
          stack.printStackTrace(new PrintWriter(writer));
          sb.append(writer);
          return sb.toString();
        }
    }

    public Object clone() throws CloneNotSupportedException {
      return new CountingCacheEventListener();
    }

    public String toString() {
      StringBuilder sb = new StringBuilder("CountingCacheEventListener\n");
      sb.append("\tElements Put:\n");
      for (CacheEvent e : elementsPut) {
        sb.append("\t\t").append(e).append("\n");
      }
      sb.append("\tElements Updated:\n");
      for (CacheEvent e : elementsUpdated) {
        sb.append("\t\t").append(e).append("\n");
      }
      sb.append("\tElements Removed:\n");
      for (CacheEvent e : elementsRemoved) {
        sb.append("\t\t").append(e).append("\n");
      }
      sb.append("\tElements Expired:\n");
      for (CacheEvent e : elementsExpired) {
        sb.append("\t\t").append(e).append("\n");
      }
      sb.append("\tElements Evicted:\n");
      for (CacheEvent e : elementsEvicted) {
        sb.append("\t\t").append(e).append("\n");
      }
      sb.append("\tRemove Alls: ").append(removeAlls.size()).append("\n");
      return sb.toString();
    }
    
    public static CountingCacheEventListener getCountingCacheEventListener(Ehcache cache) {
      for (CacheEventListener listener : cache.getCacheEventNotificationService().getCacheEventListeners()) {
        if (listener instanceof CountingCacheEventListener) {
          return (CountingCacheEventListener) listener;
        }
      }
      return null;
    }
}
