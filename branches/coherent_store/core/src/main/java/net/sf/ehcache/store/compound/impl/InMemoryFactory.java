/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.store.compound.impl;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.compound.IdentityElementProxyFactory;

/**
 * A simple unlimited capacity in-memory proxy factory.
 * 
 * @author Chris Dennis
 */
public class InMemoryFactory implements IdentityElementProxyFactory {

    /**
     * A no-op decode that just returns the unmodified element.
     */
    public Element decode(Object key, Element element) {
        return element;
    }

    /**
     * A no-op encode that just returns the unmodified element.
     */
    public Element encode(Object key, Element element) {
        return element;
    }

    /**
     * Nothing to free, so a no-op.
     */
    public void free(Element element) {
        // no-op
    }

    /**
     * Nothing to free, so a no-op.
     */
    public void freeAll() {
        // no-op
    }
}
