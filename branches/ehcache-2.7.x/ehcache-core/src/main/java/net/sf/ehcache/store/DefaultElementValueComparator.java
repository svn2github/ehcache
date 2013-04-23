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
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;

import java.util.Arrays;

/**
 * DefaultElementValueComparator does compare the values on the two elements
 * based on their {@link Object#equals(Object) equals} contract
 *
 * @author Ludovic Orban
 */
public class DefaultElementValueComparator implements ElementValueComparator {

    private final CacheConfiguration cacheConfiguration;

    /**
     * Constructor
     *
     * @param cacheConfiguration the cache configuration
     */
    public DefaultElementValueComparator(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Element e1, Element e2) {
        if (e1 == null && e2 == null) {
            return true;
        } else if (e1 != null && e1.equals(e2)) {
            if (e1.getObjectValue() == null) {
                return e2.getObjectValue() == null;
            } else {
                return compareValues(copyForReadIfNeeded(e1).getObjectValue(), copyForReadIfNeeded(e2).getObjectValue());
            }
        } else {
            return false;
        }
    }

    private static boolean compareValues(Object objectValue1, Object objectValue2) {
        if (objectValue1 != null && objectValue2 != null && objectValue1.getClass().isArray() && objectValue2.getClass().isArray()) {
            return Arrays.deepEquals(new Object[] {objectValue1}, new Object[] {objectValue2});
        } else {
            if (objectValue1 == null) {
                return objectValue2 == null;
            } else {
                return objectValue1.equals(objectValue2);
            }
        }
    }

    private Element copyForReadIfNeeded(Element element) {
        ReadWriteCopyStrategy<Element> readWriteCopyStrategy = cacheConfiguration.getCopyStrategy();
        if (readWriteCopyStrategy == null || skipCopyForRead()) {
            return element;
        }
        return readWriteCopyStrategy.copyForRead(element);
    }

    private boolean skipCopyForRead() {
        // only do a copy for read before comparing if both copy on read and copy on write are enabled
        return !(cacheConfiguration.isCopyOnRead() && cacheConfiguration.isCopyOnWrite());
    }

}
