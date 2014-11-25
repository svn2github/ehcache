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

package net.sf.ehcache.transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeExtractorException;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;

/**
 * Used to extract a search attribute value from an element in a transactional store.
 * <p>
 *
 * @author Chris Dennis
 */
public class TransactionAwareAttributeExtractor implements AttributeExtractor {

    private final ReadWriteCopyStrategy<Element> copyStrategy;
    private final AttributeExtractor delegate;

    /**
     * Creates an attributed delegating to the supplied extractor, via the given copy strategy.
     *
     * @param copyStrategy copy strategy used by the transactional store
     * @param delegate original configured attribute extractor
     */
    public TransactionAwareAttributeExtractor(ReadWriteCopyStrategy<Element> copyStrategy, AttributeExtractor delegate) {
        this.copyStrategy = copyStrategy;
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    public Object attributeFor(Element element, String attributeName) throws AttributeExtractorException {
        Object value = element.getObjectValue();
        if (value instanceof SoftLockID) {
            throw new AssertionError();
        } else {
            return delegate.attributeFor(copyStrategy.copyForRead(element), attributeName);
        }
    }
}
