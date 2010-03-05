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
 * Internal interface implemented by all element proxy factories.
 * <p>
 * This interface is package private because classes should never implement it directly.
 * Instead they should implement one of its two sub-interfaces {@link ElementProxyFactory} or
 * {@link IdentityElementProxyFactory} depending on the implementations intent.
 * 
 * @author Chris Dennis
 *
 * @param <T> the type of the proxied object (may be {@link Element} itself)
 */
interface InternalElementProxyFactory<T> {

    /**
     * Encodes the supplied {@link Element}
     * <p>
     * In the case that this element is no longer mapped to a key - if
     * for example the element is being decoded following a removal - then
     * the supplied key will be null.
     * 
     * @param key key to which this element is mapped 
     * @param element Element to encode
     * @return The potentially proxied Element
     */
    public T encode(Object key, Element element);

    /**
     * Decodes the supplied {@link Element} or {@link ElementProxy}.
     * 
     * @param key key to which this element is mapped
     * @param object Element or ElementProxy to decode
     * @return a decoded Element
     */
    public Element decode(Object key, T object);
    
    /**
     * Free any manually managed resources used by this {@link Element} or
     * {@link ElementProxy}.
     * 
     * @param object Element or ElementProxy being free'd.
     */
    public void free(T object);

    /**
     * Free all manually managed resource allocated by this factory.
     */
    public void freeAll();
}
