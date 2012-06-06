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

import net.sf.ehcache.Element;

/**
 * ReadWriteCopyStrategy adaptor for a legacy CopyStrategy instance
 *
 * @author Ludovic Orban
 */
public class LegacyCopyStrategyAdapter implements ReadWriteCopyStrategy<Element> {

    private final CopyStrategy legacyCopyStrategy;

    /**
     * create a LegacyCopyStrategyAdapter
     *
     * @param legacyCopyStrategy the legacy CopyStrategy to adapt
     */
    public LegacyCopyStrategyAdapter(CopyStrategy legacyCopyStrategy) {
        this.legacyCopyStrategy = legacyCopyStrategy;
    }

    /**
     * {@inheritDoc}
     */
    public Element copyForWrite(Element value) {
        return legacyCopyStrategy.copy(value);
    }

    /**
     * {@inheritDoc}
     */
    public Element copyForRead(Element storedValue) {
        return legacyCopyStrategy.copy(storedValue);
    }
}
