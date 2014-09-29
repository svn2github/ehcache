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
import net.sf.ehcache.config.Searchable;

/**
 * Implementation of a {@link BruteForceSource} to integrate with a {@link MemoryStore}
 *
 * @author ljacomet
 */
class MemoryStoreBruteForceSource implements BruteForceSource {

    private final MemoryStore memoryStore;
    private final Searchable searchable;

    /**
     * Constructs a {@link BruteForceSource} using a {@link MemoryStore} and the associated
     * {@link Searchable} configuration
     *
     * @param memoryStore the underlying MemoryStore
     * @param searchable the associated Searchable configuration
     */
    MemoryStoreBruteForceSource(MemoryStore memoryStore, Searchable searchable) {

        this.memoryStore = memoryStore;
        this.searchable = searchable;
    }

    @Override
    public Iterable<Element> elements() {
        return memoryStore.elementSet();
    }

    @Override
    public Searchable getSearchable() {
        return searchable;
    }

    @Override
    public Element transformForIndexing(Element element) {
        return element;
    }
}
