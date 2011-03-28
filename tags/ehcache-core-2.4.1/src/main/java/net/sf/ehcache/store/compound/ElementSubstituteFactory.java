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
 * ElementSubstituteFactory is implemented by all true substituting factories.
 * <p>
 * A true substituting factory returns an ElementSubstitute instance instead of an Element
 * on calls to encode.  ElementSubstitute instances may simply be wrapping Elements with
 * additional functionality (e.g. a soft/weak reference) or be indirectly referencing
 * an Element (e.g. a pointer to a secondary storage location).
 * 
 * @author Chris Dennis
 *
 * @param <T> type of the created and retrievable element substitutes.
 */
public interface ElementSubstituteFactory<T extends ElementSubstitute> extends InternalElementSubstituteFactory<T> {

    /**
     * @return The substitute element
     * 
     * @throws IllegalArgumentException if element cannot be substituted
     */
    public T create(Object key, Element element) throws IllegalArgumentException;

    /**
     * Decodes the supplied {@link ElementSubstitute}.
     * 
     * @param object ElementSubstitute to decode
     */
    public Element retrieve(Object key, T object);
    
    /**
     * Free any manually managed resources used by this {@link ElementSubstitute}.
     * 
     * @param object ElementSubstitute being free'd.
     */
    public void free(Lock exclusion, T object);
}
