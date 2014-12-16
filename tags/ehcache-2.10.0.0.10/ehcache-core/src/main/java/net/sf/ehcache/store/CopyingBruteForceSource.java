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

import java.util.Iterator;

/**
 * A {@link BruteForceSource} that wraps another one and deals with copy for read of {@link Element}s.
 *
 * @author ljacomet
 */
class CopyingBruteForceSource implements BruteForceSource {

    private final BruteForceSource delegate;
    private final CopyStrategyHandler copyStrategyHandler;

    /**
     * Construct this CopyingBruteForceSource with the given delegate and the {@link CopyStrategyHandler} to use.
     *
     * @param delegate the delegate BruteForceSource
     * @param copyStrategyHandler the copy strategy handler
     */
    CopyingBruteForceSource(BruteForceSource delegate, CopyStrategyHandler copyStrategyHandler) {
        this.delegate = delegate;
        this.copyStrategyHandler = copyStrategyHandler;
    }

    @Override
    public Iterable<Element> elements() {
        return new CopyingIterable(delegate.elements(), copyStrategyHandler);
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
    private static class CopyingIterable implements Iterable<Element> {
        private final Iterable<Element> elements;
        private final CopyStrategyHandler copyStrategyHandler;

        public CopyingIterable(Iterable<Element> elements, CopyStrategyHandler copyStrategyHandler) {
            this.elements = elements;
            this.copyStrategyHandler = copyStrategyHandler;
        }

        @Override
        public Iterator<Element> iterator() {
            return new CopyingIterator(elements.iterator());
        }

        /**
         * Wrapping Iterator responsible of doing the copy on read
         */
        private class CopyingIterator implements Iterator<Element> {
            private final Iterator<Element> delegate;

            public CopyingIterator(Iterator<Element> delegate) {
                this.delegate = delegate;
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Element next() {
                return copyStrategyHandler.copyElementForReadIfNeeded(delegate.next());
            }

            @Override
            public void remove() {
                delegate.remove();
            }
        }
    }
}
