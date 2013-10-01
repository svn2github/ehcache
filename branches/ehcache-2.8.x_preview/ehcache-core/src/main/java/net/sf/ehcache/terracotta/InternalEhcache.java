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

package net.sf.ehcache.terracotta;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * This is an extension of the Ehcache interface to allow addition of new methods to Cache without breaking the public contract.
 *
 * The Cache class implements InternalEhcache interface, which in turn extends the Ehcahce interface.
 *
 * @author dkumar
 *
 */

public interface InternalEhcache extends Ehcache {

    /**
     * Removes and returns the element associated with the key
     *
     * @param key the key of the element to operate on
     * @return element the removed element associated with the key, null if no mapping exists
     * @throws IllegalStateException
     */
    Element removeAndReturnElement(Object key) throws IllegalStateException;

    /**
     * Recalculate the size of the element mapped to the key
     * @param key the key
     */
    void recalculateSize(Object key);

}
