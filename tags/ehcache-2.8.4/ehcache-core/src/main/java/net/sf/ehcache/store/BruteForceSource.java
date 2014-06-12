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
 * This interface exposes what is required by the {@link BruteForceSearchManager}
 *
 * @author ljacomet
 */
interface BruteForceSource {

    /**
     * An {@link Iterable} over the elements from the source.
     * The elements returned are in the form expected by the {@link BruteForceSearchManager}.
     *
     * @return an Iterable of Element
     */
    Iterable<Element> elements();

    /**
     * Returns the {@link Searchable} configuration of the source.
     *
     * @return the Searchable configuration
     */
    Searchable getSearchable();

    /**
     * Transform the element so that it can be used for indexing by the {@link BruteForceSearchManager}.
     *
     * @param element the element to transform
     * @return the transformed element
     */
    Element transformForIndexing(Element element);
}
