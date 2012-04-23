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

package net.sf.ehcache.constructs.nonstop.concurrency;

import java.util.LinkedList;

/**
 * A class that maintains a thread local to keep track of any explicit locks acquired by the app thread
 *
 * @author Abhishek Sanoujam
 *
 */
public final class ExplicitLockingContextThreadLocal {

    private final ThreadLocal<ExplicitLockingContext> contextThreadLocal = new ThreadLocal<ExplicitLockingContext>() {
        @Override
        protected ExplicitLockingContextImpl initialValue() {
            return new ExplicitLockingContextImpl();
        }
    };

    /**
     * Public constructor
     */
    public ExplicitLockingContextThreadLocal() {
        // private constructor
    }

    /**
     * Returns true if the thread has acquired any locks previously and not released yet
     *
     * @return true if the thread has acquired any locks previously and not released yet
     */
    public boolean areAnyExplicitLocksAcquired() {
        return contextThreadLocal.get().areAnyExplicitLocksAcquired();
    }

    /**
     * Returns the thread local for the current thread
     *
     * @return the thread local for the current thread
     */
    public ExplicitLockingContext getCurrentThreadLockContext() {
        return contextThreadLocal.get();
    }

    /**
     *
     * This class doesn't need to be thread safe as there's 1-1 mapping between app thread and nonstop threads. And only one of the threads
     * is using this class at a time.
     *
     */
    private static class ExplicitLockingContextImpl implements ExplicitLockingContext {

        // volatile for visibility
        private volatile boolean locksAcquired;
        private final LinkedList<Long> lockAcquisitionStack = new LinkedList<Long>();

        /**
         * {@inheritDoc}
         */
        public boolean areAnyExplicitLocksAcquired() {
            return locksAcquired;
        }

        /**
         * {@inheritDoc}
         */
        public void lockAcquired(final long currentNonstopThreadUniqueId) {
            locksAcquired = true;
            lockAcquisitionStack.addFirst(Long.valueOf(currentNonstopThreadUniqueId));
        }

        /**
         * {@inheritDoc}
         */
        public void lockReleased() {
            if (lockAcquisitionStack.peek() != null) {
                lockAcquisitionStack.removeFirst();
            }
            // a litte perf than doing size() > 0
            if (lockAcquisitionStack.peek() == null) {
                locksAcquired = false;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean areLocksAcquiredByOtherThreads(long currentNonstopThreadUniqueId) {
            for (Long nonstopThreadUniqueId : lockAcquisitionStack) {
                if (nonstopThreadUniqueId.longValue() != currentNonstopThreadUniqueId) {
                    return true;
                }
            }
            return false;
        }
    }
}
