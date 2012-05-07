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
 * An adapter to simplify cache event listeners that do not need all events defined on the CacheEventListener interface.
 *
 * @author Ludovic Orban
 */
public class CacheEventListenerAdapter implements CacheEventListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(Ehcache cache) {
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
    }
}
