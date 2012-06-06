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
package net.sf.ehcache.store.disk;

import net.sf.ehcache.writer.CacheWriterManagerException;

/**
 * Exception thrown by the Store when the writer fails. Used to determine whether the element was inserted or updated in the Store
 * @author Alex Snaps
 */
public class StoreUpdateException extends CacheWriterManagerException {

    private final boolean update;

    /**
     * Constructor
     * @param e the cause of the failure
     * @param update true if element was updated, false if inserted
     */
    public StoreUpdateException(final RuntimeException e, final boolean update) {
        super(e);
        this.update = update;
    }

    /**
     * Whether the element was inserted or updated in the Store
     * @return true if element was updated, false if inserted
     */
    public boolean isUpdate() {
        return update;
    }
}
