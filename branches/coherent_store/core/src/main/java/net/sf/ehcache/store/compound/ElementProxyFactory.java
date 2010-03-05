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

package net.sf.ehcache.store.compound;

import net.sf.ehcache.Element;

/**
 * ElementProxyFactory is implemented by all true proxying factories.
 * <p>
 * A true proxying factory returns an ElementProxy instance instead of an Element
 * on calls to encode.  ElementProxy instances may simply be wrapping Elements with
 * additional functionality (e.g. a soft/weak reference) or be indirectly referencing
 * an Element (e.g. a pointer to a secondary storage location).
 * 
 * @author Chris Dennis
 *
 * @param <T> type of the encoded and decodable element proxies.
 */
public interface ElementProxyFactory<T extends ElementProxy> extends InternalElementProxyFactory<T> {

    /**
     * @return The proxied Element
     */
    public T encode(Object key, Element element);

    /**
     * Decodes the supplied {@link ElementProxy}.
     * 
     * @param object ElementProxy to decode
     */
    public Element decode(Object key, T object);
    
    /**
     * Free any manually managed resources used by this {@link ElementProxy}.
     * 
     * @param object ElementProxy being free'd.
     */
    public void free(T object);
}
