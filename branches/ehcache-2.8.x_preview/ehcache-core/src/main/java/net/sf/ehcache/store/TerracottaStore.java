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

import java.util.Set;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.writer.writebehind.WriteBehind;

/**
 * Use for internal purpose only. Teaser: Stores of Terracotta clustered caches implements this interface.
 *
 * @author Abhishek Sanoujam
 *
 */
public interface TerracottaStore extends Store {

    /**
     * Returns the local value associated with the key. Local value means that the object mapped to the key is present in the VM locally. In
     * case its not, will return null. Note that even when returning null, the value may be present in the Terracotta server array.
     * <p/>
     * This operation does not acquire any locks when doing the operation and may return stale values.
     * This Operation does not update last usage statistics
     *
     * @param key
     *            the key
     * @return the element associated with key or null
     */
    public Element unsafeGet(Object key);

    /**
     * Returns set of keys from the cache which are present in the node locally.
     *
     * @return set of keys present locally in the node
     */
    public Set getLocalKeys();


    /**
     * Get the transactional mode of this store. The returned value is the
     * String value of CacheConfiguration.TransactionalMode.
     *
     * @return a String representation of this store's transactional mode.
     * @see net.sf.ehcache.config.CacheConfiguration.TransactionalMode
     */
    public CacheConfiguration.TransactionalMode getTransactionalMode();

    /**
     * {@link WriteBehind} createWriteBehind
     */
    public WriteBehind createWriteBehind();

}
