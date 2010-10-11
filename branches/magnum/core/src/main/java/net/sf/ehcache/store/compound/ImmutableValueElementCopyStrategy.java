/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import net.sf.ehcache.Element;

/**
 * @author Alex Snaps
 */
public class ImmutableValueElementCopyStrategy implements CopyStrategy {

    private CopyStrategy defaultCopyStrategy = new SerializationCopyStrategy();

    /**
     * @inheritDoc
     */
    public <T> T copy(final T value) {

        final T newValue;
        if (value instanceof Element) {
            Element element = (Element) value;
            Element newElement = new Element(element.getObjectKey(), element.getObjectValue(), element.getVersion(),
                element.getCreationTime(), element.getLastAccessTime(), element.getHitCount(), element.usesCacheDefaultLifespan(),
                element.getTimeToLive(), element.getTimeToIdle(), element.getLastUpdateTime());
            newValue = (T) newElement;
        } else {
            newValue = defaultCopyStrategy.copy(value);
        }

        return newValue;
    }
}
