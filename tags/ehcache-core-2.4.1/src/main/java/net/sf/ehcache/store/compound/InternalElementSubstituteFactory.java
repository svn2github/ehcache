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

package net.sf.ehcache.store.compound;

import java.util.concurrent.locks.Lock;

import net.sf.ehcache.Element;

/**
 * Internal interface implemented by all element substitute factories.
 * <p>
 * This interface is package private because classes should never implement it directly.
 * Instead they should implement one of its two sub-interfaces {@link ElementSubstituteFactory} or
 * {@link IdentityElementSubstituteFactory} depending on the implementations intent.
 * 
 * @author Chris Dennis
 *
 * @param <T> the type of the substitute object (may be {@link Element} itself)
 */
interface InternalElementSubstituteFactory<T> {

    /**
     * Bind a store instance to this factory.
     * 
     * @param store store to bind
     */
    public void bind(CompoundStore store);

    /**
     * Unbinds a store instance from this factory
     * @param store store to unbind
     */
    public void unbind(CompoundStore store);

    /**
     * Creates a substitute for the supplied {@link Element}
     * <p>
     * In the case that this element is no longer mapped to a key - if
     * for example the element is being decoded following a removal - then
     * the supplied key will be null.
     * 
     * @param key key to which this element is mapped 
     * @param element Element to encode
     * @return The potentially substitute Element
     */
    public T create(Object key, Element element);

    /**
     * Retrieves the supplied {@link Element} or {@link ElementSubstitute}.
     * 
     * @param key key to which this element is mapped
     * @param object Element or ElementSubstitute to retrieve
     * @return a decoded Element
     */
    public Element retrieve(Object key, T object);
    
    /**
     * Free any manually managed resources used by this {@link Element} or
     * {@link ElementSubstitute}.
     * 
     * @param object Element or ElementSubstitute being free'd.
     */
    public void free(Lock exclusion, T object);

    /**
     * Returns <code>true</code> if this factory created the given object.
     * 
     * @param object object to check
     * @return <code>true</code> if object created by this factory
     */
    public boolean created(Object object);
}
