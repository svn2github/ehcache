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
import net.sf.ehcache.transaction.SoftLockID;

import java.util.Iterator;

/**
 * A {@link BruteForceSource} that wraps another one and deals with transactional {@link Element}s.
 *
 * @author ljacomet
 */
class TransactionalBruteForceSource implements BruteForceSource {
    private final BruteForceSource delegate;
    private final CopyStrategyHandler copyStrategyHandler;

    /**
     * A wrapping {@link BruteForceSource} that deals with transactional elements
     *
     * @param delegate the delegate source
     * @param copyStrategyHandler the {@link CopyStrategyHandler} to use
     */
    TransactionalBruteForceSource(BruteForceSource delegate, CopyStrategyHandler copyStrategyHandler) {
        this.delegate = delegate;
        this.copyStrategyHandler = copyStrategyHandler;
    }

    @Override
    public Iterable<Element> elements() {
        return new TransactionalIterable(delegate.elements(), copyStrategyHandler);
    }

    @Override
    public Searchable getSearchable() {
        return delegate.getSearchable();
    }

    @Override
    public Element transformForIndexing(Element element) {
        return copyStrategyHandler.copyElementForReadIfNeeded(element);
    }

    /**
     * Wrapping Iterable holding the delegate Iterable and the {@link CopyStrategyHandler}
     */
    private static class TransactionalIterable implements Iterable<Element> {
        private final Iterable<Element> elements;
        private final CopyStrategyHandler copyStrategyHandler;

        public TransactionalIterable(Iterable<Element> elements, CopyStrategyHandler copyStrategyHandler) {
            this.elements = elements;
            this.copyStrategyHandler = copyStrategyHandler;
        }

        @Override
        public Iterator<Element> iterator() {
            return new TransactionalIterator(elements.iterator());
        }

        /**
         * Wrapping Iterator responsible of handling transactional elements and doing the copy on read
         */
        private class TransactionalIterator implements Iterator<Element> {
            private final Iterator<Element> delegate;
            private Element next = null;

            public TransactionalIterator(Iterator<Element> delegate) {
                this.delegate = delegate;
                next = getNextElement();
            }

            private Element getNextElement() {
                while (delegate.hasNext()) {
                    Element candidate = delegate.next();
                    if (candidate.getObjectValue() instanceof SoftLockID) {
                        candidate = ((SoftLockID)candidate.getObjectValue()).getOldElement();
                    }
                    if (candidate != null) {
                        return candidate;
                    }
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Element next() {
                Element result = next;
                next = getNextElement();
                return copyStrategyHandler.copyElementForReadIfNeeded(result);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
