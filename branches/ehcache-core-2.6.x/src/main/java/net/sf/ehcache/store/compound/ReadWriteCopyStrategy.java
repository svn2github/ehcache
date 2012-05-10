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

package net.sf.ehcache.store.compound;

import java.io.Serializable;

/**
 * @param <T> type
 * @since 2.4.0
 * @author Ludovic Orban
 */
public interface ReadWriteCopyStrategy<T> extends Serializable {

    /**
     * Deep copies some object and returns an internal storage-ready copy
     *
     * @param value the value to copy
     * @return the storage-ready copy
     */
    T copyForWrite(final T value);

    /**
     * Reconstruct an object from its storage-ready copy.
     *
     * @param storedValue the storage-ready copy
     * @return the original object
     */
    T copyForRead(final T storedValue);
}
