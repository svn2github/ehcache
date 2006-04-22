/**
 *  Copyright 2003-2006 Greg Luck
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Null Object Pattern implementation of CacheEventListener. It simply logs the calls made.
 * <p/>
 * It is used by default.
 *
 * @author Greg Luck
 * @version $Id$
 * @since 1.2
 */
public class NullCacheEventListener implements CacheEventListener {

    private static final Log LOG = LogFactory.getLog(NullCacheEventListener.class.getName());


    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(final Cache cache, final Element element) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("notifyElementRemoved called for cache " + cache + " for element with key " + element.getObjectKey());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(final Cache cache, final Element element) {
        if (LOG.isTraceEnabled()) {
            Object key = null;
            if (element != null) {
                key = element.getObjectKey();
            }
            LOG.trace("notifyElementPut called for cache " + cache + " for element with key " + key);
        }
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
    public void notifyElementUpdated(final Cache cache, final Element element) throws CacheException {
         if (LOG.isTraceEnabled()) {
            Object key = null;
            if (element != null) {
                key = element.getObjectKey();
            }
            LOG.trace("notifyElementUpdated called for cache " + cache + " for element with key " + key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(final Cache cache, final Element element) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("notifyElementExpired called for cache " + cache + " for element with key " + element.getObjectKey());
        }
    }

    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     */
    public void dispose() {
        //nothing to do
    }
}
