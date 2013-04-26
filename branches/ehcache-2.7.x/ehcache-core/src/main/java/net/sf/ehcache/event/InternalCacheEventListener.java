/**
 *  Copyright Terracotta, Inc.
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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * Ehcache internal interface for listening on cache events.
 *
 * @author Alex Snaps
 */
interface InternalCacheEventListener extends Cloneable {

    /**
     * Called immediately after an element removal.
     * <p/>
     * The method causing the remove will block until this method returns.
     * <p/>
     * This notification is not called for the following special case:
     * <ol>
     * <li>removeAll was called
     * </ol>
     *
     * @param cache   the cache emitting the notification
     * @param element the element just deleted.
     */
    void notifyElementRemoved(Ehcache cache, Element element) throws CacheException;

    /**
     * Called immediately after an element has been put into the cache.
     * <p/>
     * The method causing the put will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the
     * element is provided. Implementers should be careful not to modify the element. The
     * effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    void notifyElementPut(Ehcache cache, Element element) throws CacheException;

    /**
     * Give the listener a chance to cleanup and free resources when no longer needed
     */
    void dispose();
}
