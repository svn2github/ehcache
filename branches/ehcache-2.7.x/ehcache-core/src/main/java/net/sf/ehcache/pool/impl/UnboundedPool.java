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

package net.sf.ehcache.pool.impl;

import java.util.Collection;
import java.util.Collections;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolParticipant;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * A no-op pool which does not enforce any resource consumption limit.
 *
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public class UnboundedPool implements Pool {

    /**
     * An accessor that just is unbounded
     */
    public static final PoolAccessor<PoolParticipant> UNBOUNDED_ACCESSOR = new UnboundedPoolAccessor();

    /**
     * Create an UnboundedPool instance
     */
    public UnboundedPool() {
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxSize() {
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxSize(long newSize) {
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor createPoolAccessor(PoolParticipant participant, int maxDepth, boolean abortWhenMaxDepthExceeded) {
        return new UnboundedPoolAccessor();
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor createPoolAccessor(PoolParticipant participant, SizeOfEngine sizeOfEngine) {
        return new UnboundedPoolAccessor();
    }

    /**
     * {@inheritDoc}
     */
    public void registerPoolAccessor(PoolAccessor accessor) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    public void removePoolAccessor(PoolAccessor accessor) {
        //no-op
    }

    /**
     * {@inheritDoc}
     */
    public Collection<PoolAccessor> getPoolAccessors() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public PoolEvictor getEvictor() {
        throw new UnsupportedOperationException();
    }

    /**
     * The PoolAccessor class of the UnboundedPool
     */
    private static final class UnboundedPoolAccessor implements PoolAccessor<PoolParticipant> {

        private UnboundedPoolAccessor() {
        }

        /**
         * {@inheritDoc}
         */
        public long add(Object key, Object value, Object container, boolean force) {
            return 0L;
        }

        /**
         * {@inheritDoc}
         */
        public boolean canAddWithoutEvicting(Object key, Object value, Object container) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public long delete(long sizeOf) throws IllegalArgumentException {
//            if (sizeOf < 0L) {
//                throw new IllegalArgumentException("cannot delete negative size");
//            }
            return 0L;
        }

        /**
         * {@inheritDoc}
         */
        public long replace(long currentSize, Object key, Object value, Object container, boolean force) {
            return 0L;
        }

        /**
         * {@inheritDoc}
         */
        public long getSize() {
            return -1L;
        }

        /**
         * {@inheritDoc}
         */
        public void unlink() {
        }

        /**
         * {@inheritDoc}
         */
        public void clear() {
        }

        /**
         * {@inheritDoc}
         */
        public PoolParticipant getParticipant() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public void setMaxSize(final long newValue) {
        }

        /**
         * {@inheritDoc}
         */
        public long getPoolOccupancy() {
            return -1L;
        }

        /**
         * {@inheritDoc}
         */
        public long getPoolSize() {
            return Long.MAX_VALUE;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasAbortedSizeOf() {
            return false;
        }
    }
}
