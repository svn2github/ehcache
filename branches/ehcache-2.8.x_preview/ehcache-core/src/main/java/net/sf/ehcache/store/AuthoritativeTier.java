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
package net.sf.ehcache.store;

import net.sf.ehcache.Element;

/**
 * Document me
 *
 * @author Alex Snaps
 */
public interface AuthoritativeTier extends Store {

    /**
     * Marks the entry as not evictable and returns it atomically
     *
     * @param key
     * @param updateStats
     * @return
     */
    Element fault(Object key, boolean updateStats);

    /**
     * Stupid "implicit" contract in tests that dictates that entries put, will be in highest tier!
     *
     * @param element
     * @return
     */
    @Deprecated
    boolean putFaulted(Element element);

    /**
     * This marks the entry as evictable again and updates relevant access stats
     *
     * @param element
     * @return
     */
    boolean flush(Element element);

}
