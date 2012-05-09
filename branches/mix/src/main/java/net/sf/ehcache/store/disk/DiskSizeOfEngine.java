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

import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * SizeOf engine which calculates exact usage of the disk store.
 *
 * @author Ludovic Orban
 */
public class DiskSizeOfEngine implements SizeOfEngine {

    /**
     * {@inheritDoc}
     */
    public Size sizeOf(Object key, Object value, Object container) {
        if (container != null && !(container instanceof DiskStorageFactory.DiskMarker)) {
            throw new IllegalArgumentException("can only size DiskStorageFactory.DiskMarker");
        }

        if (container == null) {
            return new Size(0, true);
        }

        DiskStorageFactory.DiskMarker marker = (DiskStorageFactory.DiskMarker) container;
        return new Size(marker.getSize(), true);
    }

    /**
     * {@inheritDoc}
     */
    public SizeOfEngine copyWith(int maxDepth, boolean abortWhenMaxDepthExceeded) {
        return new DiskSizeOfEngine();
    }

}
