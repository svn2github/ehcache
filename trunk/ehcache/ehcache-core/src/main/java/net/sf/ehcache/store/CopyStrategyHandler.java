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
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;

/**
 * Utility class for handling element copy based on the copy on read and write configurations.
 *
 * @author ljacomet
 */
public final class CopyStrategyHandler {

    private final boolean copyOnRead;
    private final boolean copyOnWrite;
    private final ReadWriteCopyStrategy<Element> copyStrategy;
    private final ClassLoader loader;


    /**
     * Creates a CopyStrategyHandler based on the copy configuration
     *
     * @param copyOnRead copy on read flag
     * @param copyOnWrite copy on write flag
     * @param copyStrategy the copy strategy to use
     */
    public CopyStrategyHandler(boolean copyOnRead, boolean copyOnWrite, ReadWriteCopyStrategy<Element> copyStrategy, ClassLoader loader) {
        this.copyOnRead = copyOnRead;
        this.copyOnWrite = copyOnWrite;
        this.copyStrategy = copyStrategy;
        this.loader = loader;
        if (isCopyActive() && this.copyStrategy == null) {
            throw new IllegalArgumentException("Copy strategy cannot be null with copyOnRead or copyOnWrite true");
        }
    }

    /**
     * Perform copy on read on an element if configured
     *
     * @param element the element to copy for read
     * @return a copy of the element with the reconstructed original value
     */
    public Element copyElementForReadIfNeeded(Element element) {
        if (element == null) {
            return null;
        }

        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForRead(element, loader);
        } else if (copyOnRead) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element, loader), loader);
        } else {
            return element;
        }
    }

    /**
     * Perform copy on write on an element if configured
     *
     * @param element the element to copy for write
     * @return a copy of the element with a storage-ready value
     */
    Element copyElementForWriteIfNeeded(Element element) {
        if (element == null) {
            return null;
        }

        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForWrite(element, loader);
        } else if (copyOnWrite) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element, loader), loader);
        } else {
            return element;
        }
    }

    /**
     * Perform copy for the element If both copy on read and copy on write are set to true
     *
     * @param element the element to copy for removal
     * @return a copy of the element with a storage-ready value
     */
    Element copyElementForRemovalIfNeeded(Element element) {
        if (element == null) {
            return null;
        }

        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForWrite(element, loader);
        } else {
            return element;
        }
    }

    /**
     * Returns wether copyOnRead or copyOnWrite is set
     *
     * @return true if copy needs to happen
     */
    boolean isCopyActive() {
        return copyOnRead || copyOnWrite;
    }

}
