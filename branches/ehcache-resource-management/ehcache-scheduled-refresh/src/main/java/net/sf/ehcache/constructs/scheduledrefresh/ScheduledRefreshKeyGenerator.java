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
package net.sf.ehcache.constructs.scheduledrefresh;

import net.sf.ehcache.Ehcache;

import java.io.Serializable;

/**
 * Interface for generating keys from a cache.
 *
 * @param <K>
 * @author cschanck
 */
public interface ScheduledRefreshKeyGenerator<K extends Serializable> {

    /**
     * Return an {@link Iterable} of all the keys to be refreshed.
     * Note that the cache will almost certainly be modified while this Iterable is
     * being traversed if the cache is of any size at all.
     *
     * @param cache
     * @return Iterable of keys to refresh. This can be formed in any way, lazily, as a
     * copy, it doesn't matter. No claims are made with respect to the visibility of other
     * chages as this iteration is done; it merely must be best-effort at the time.
     */
    public Iterable<K> generateKeys(Ehcache cache);
}
