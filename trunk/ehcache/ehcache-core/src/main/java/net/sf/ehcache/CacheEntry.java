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

package net.sf.ehcache;

/**
 * Helper class to tie a key to an element.
 * <p/>
 * This is used for operations that are identified by a key but that could benefit from additional information that's
 * available in an element when it can be found in the cache. If the element isn't available, it will be {@code null}.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class CacheEntry {
    private final Object key;
    private final Element element;

    /**
     * Creates a new cache entry.
     *
     * @param key the key of the entry
     * @param element the element of the entry or {@code null} if no element corresponds to the key at this time
     */
    public CacheEntry(Object key, Element element) {
        this.key = key;
        this.element = element;
    }

    /**
     * Retrieves the key of this cache entry.
     *
     * @return the request key
     */
    public Object getKey() {
        return key;
    }

    /**
     * Retrieves the element of this cache entry.
     *
     * @return the element that corresponds to this key or {@code null} if the cache entry didn't have an element that
     * belong to the key at the time of creation.
     */
    public Element getElement() {
        return element;
    }
}
