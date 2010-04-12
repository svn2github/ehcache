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

package net.sf.ehcache.store;

import net.sf.ehcache.Element;

/**
 * Use for internal purpose only.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public interface UnsafeStore extends Store {

    /**
     * Returns the local value associated with the key
     * 
     * @param key
     *            the key
     * @return the element associated with key or null
     */
    public Element unsafeGet(Object key);

    /**
     * Returns the local value associated with the key.
     * Same as {{@link #unsafeGet(Object)} but does not update statistics
     * 
     * @param key
     *            the key
     * @return the element associated with key or null
     */
    public Element unsafeGetQuiet(Object key);

    /**
     * Do a dirty read for the key
     * 
     * @param key
     * @return the element associated with the key or null
     */
    public Element unlockedGet(Object key);

    /**
     * Same as {{@link #unlockedGet(Object)} but does not update statistics
     * 
     * @param key
     * @return the element associated with the key or null
     */
    public Element unlockedGetQuiet(Object key);

}
