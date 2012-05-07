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

package net.sf.ehcache.constructs.nonstop.concurrency;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;

/**
 * implementation which does not block threads when the cluster goes down
 * @author rsingh
 *
 */
public class NonStopCacheKeySet extends AbstractList {
    private final NonstopStore nonstopStore;
    private final NonstopActiveDelegateHolder nonstopActiveDelegateHolder;
    private final List keys;

    /**
     * Non stop store to iterate over key set
     * @param nonstopActiveDelegateHolder
     * @param list
     */
    public NonStopCacheKeySet(NonstopActiveDelegateHolder nonstopActiveDelegateHolder, List keys) {
        this.nonstopStore = nonstopActiveDelegateHolder.getNonstopStore();
        this.nonstopActiveDelegateHolder = nonstopActiveDelegateHolder;
        this.keys = keys;
    }

    /**
     * Iterator to iterate over the key set
     * May time out
     */
    @Override
    public Iterator iterator() {
        return new NonStopCacheKeySetIterator(nonstopStore, keys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return this.nonstopStore.executeClusterOperation(new ClusterOperation<Integer>() {

            public Integer performClusterOperation() throws Exception {
                return keys.size();
            }

            public Integer performClusterOperationTimedOut(TimeoutBehaviorType configuredTimeoutBehavior) {
                switch(configuredTimeoutBehavior) {
                    case LOCAL_READS:
                        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getLocalKeys().size();
                    case NOOP:
                        return 0;
                    case EXCEPTION:
                        throw new NonStopCacheException("keySet.size() timed out");
                    default:
                        throw new AssertionError("configuredTimeoutBehavior of unknown type");
                }
            }
        });
    }

    /**
     * Iterator to iterate over the key set using non stop executors
     * @author rsingh
     *
     */
    private static class NonStopCacheKeySetIterator implements Iterator {
        private final NonstopStore nonstopStore;
        private final Iterator iterator;

        public NonStopCacheKeySetIterator(final NonstopStore nonstopStore, final List keys) {
            this.nonstopStore = nonstopStore;
            this.iterator = this.nonstopStore.executeClusterOperation(new ClusterOperation<Iterator>() {

                public Iterator performClusterOperation() throws Exception {
                    return keys.iterator();
                }

                public Iterator performClusterOperationTimedOut(TimeoutBehaviorType configuredTimeoutBehavior) {
                    throw new NonStopCacheException("keySet timed out");
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return this.nonstopStore.executeClusterOperation(new ClusterOperation<Boolean>() {

                public Boolean performClusterOperation() throws Exception {
                    return iterator.hasNext();
                }

                public Boolean performClusterOperationTimedOut(TimeoutBehaviorType configuredTimeoutBehavior) {
                    throw new NonStopCacheException("hasNext on keySet timed out");
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        public Object next() {
            return this.nonstopStore.executeClusterOperation(new ClusterOperation<Object>() {

                public Object performClusterOperation() throws Exception {
                    return iterator.next();
                }

                public Object performClusterOperationTimedOut(TimeoutBehaviorType configuredTimeoutBehavior) {
                    throw new NonStopCacheException("next on keySet timed out");
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(int index) {
        throw new UnsupportedOperationException();
    }
}
