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

package net.sf.ehcache.pool.sizeof;

import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.sizeof.ObjectGraphWalker.Visitor;
import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;
import net.sf.ehcache.util.WeakIdentityConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract sizeOf for Java. It will rely on a proper sizeOf to measure sizes of entire object graphs
 * @author Alex Snaps
 */
public abstract class SizeOf {

    private static final Logger LOG = LoggerFactory.getLogger(SizeOf.class.getName());

    private final ObjectGraphWalker walker;

    /**
     * Builds a new SizeOf that will filter fields according to the provided filter
     * @param fieldFilter The filter to apply
     * @param caching whether to cache reflected fields
     * @see SizeOfFilter
     */
    public SizeOf(SizeOfFilter fieldFilter, boolean caching) {
        ObjectGraphWalker.Visitor visitor;
        if (caching) {
            visitor = new CachingSizeOfVisitor();
        } else {
            visitor = new SizeOfVisitor();
        }
        this.walker = new ObjectGraphWalker(visitor, fieldFilter);
    }

    /**
     * Calculates the size in memory (heap) of the instance passed in, not navigating the down graph
     *
     * @param obj the object to measure the size of
     * @return the object size in memory in bytes
     */
    public long sizeOf(Object obj) {
        if (isSharedFlyweight(obj)) {
            return 0;
        } else {
            return measureSizeOf(obj);
        }
    }

    /**
     * Measure the size of an instance
     * @param obj the reference to measure
     * @return the size occupied on heap in bytes
     */
    protected abstract long measureSizeOf(Object obj);

    /**
     * Measures the size in memory (heap) of the objects passed in, walking their graph down
     * Any overlap of the graphs being passed in will be recognized and only measured once
     *
     * @param maxDepth maximum depth of the object graph to traverse
     * @param abortWhenMaxDepthExceeded true if the object traversal should be aborted when the max depth is exceeded
     * @param obj the root objects of the graphs to measure
     * @return the total size in bytes for these objects
     * @see #sizeOf(Object)
     */
    public Size deepSizeOf(int maxDepth, boolean abortWhenMaxDepthExceeded, Object... obj) {
        try {
            return new Size(walker.walk(maxDepth, abortWhenMaxDepthExceeded, obj), true);
        } catch (MaxDepthExceededException e) {
            LOG.warn(e.getMessage());
            return new Size(e.getMeasuredSize(), false);
        }
    }

    private static boolean isSharedFlyweight(Object obj) {
        FlyweightType type = FlyweightType.getFlyweightType(obj.getClass());
        return type != null && type.isShared(obj);
    }

    /**
     * Will return the sizeOf each instance
     */
    private class SizeOfVisitor implements Visitor {

        /**
         * {@inheritDoc}
         */
        public long visit(Object object) {
            return sizeOf(object);
        }
    }

    /**
     * Will Cache already visited types
     */
    private class CachingSizeOfVisitor implements Visitor {
        private final WeakIdentityConcurrentMap<Class<?>, Long> cache = new WeakIdentityConcurrentMap<Class<?>, Long>();

        /**
         * {@inheritDoc}
         */
        public long visit(final Object object) {
            Class<?> klazz = object.getClass();
            Long cachedSize = cache.get(klazz);
            if (cachedSize == null) {
                if (klazz.isArray()) {
                    return measureSizeOf(object);
                } else if (isSharedFlyweight(object)) {
                    return 0;
                } else {
                    long size = measureSizeOf(object);
                    cache.put(klazz, size);
                    return size;
                }
            } else {
                return cachedSize.longValue();
            }
        }
    }
}
