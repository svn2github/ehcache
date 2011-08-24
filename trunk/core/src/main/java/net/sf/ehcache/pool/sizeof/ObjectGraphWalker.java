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

import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Stack;

import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;
import net.sf.ehcache.util.WeakIdentityConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will walk an object graph and let you execute some "function" along the way
 * @author Alex Snaps
 */
final class ObjectGraphWalker {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectGraphWalker.class);
    private static final String TC_INTERNAL_FIELD_PREFIX = "$__tc_";

    private static final String ABORTION_MESSAGE = "When trying to calculate the size of on-heap objects, we followed {0} references and " +
                                                   "still aren't done.\n" +
               " you should consider using the @IgnoreSizeOf annotation to set some stop points somewhere in you object graph,\n" +
               " or raise the amount of references which is allowed to follow before you get a warning (by adding a\n" +
               " <sizeOfPolicy maxDepth=\"[new value]\"/> either to your cache manager or to your cache) or stop using size-based,\n" +
               " auto-tuned caches and use count-based ones instead.";

    // Todo this is probably not what we want...
    private final WeakIdentityConcurrentMap<Class<?>, SoftReference<Collection<Field>>> fieldCache =
            new WeakIdentityConcurrentMap<Class<?>, SoftReference<Collection<Field>>>();
    private final WeakIdentityConcurrentMap<Class<?>, Boolean> classCache =
            new WeakIdentityConcurrentMap<Class<?>, Boolean>();

    private final SizeOfFilter sizeOfFilter;

    private final Visitor visitor;

    /**
     * The visitor to execute the function on each node of the graph
     * This is only to be used for the sizing of an object graph in memory!
     */
    static interface Visitor {
        /**
         * The visit method executed on each node
         * @param object the reference at that node
         * @return a long for you to do things with...
         */
        public long visit(Object object);
    }


    /**
     * Constructor
     * @param visitor the visitor to use
     * @param filter the filtering
     * @see Visitor
     * @see SizeOfFilter
     */
    ObjectGraphWalker(Visitor visitor, SizeOfFilter filter) {
        this.visitor = visitor;
        this.sizeOfFilter = filter;
    }

    /**
     * Walk the graph and call into the "visitor"
     * @param maxDepth maximum depth to traverse the object graph
     * @param abortWhenMaxDepthExceeded true if the object traversal should be aborted when the max depth is exceeded
     * @param root the roots of the objects (a shared graph will only be visited once)
     * @return the sum of all Visitor#visit returned values
     */
    long walk(int maxDepth, boolean abortWhenMaxDepthExceeded, Object... root) {
        long result = 0;
        boolean warned = false;
        try {
            Stack<Object> toVisit = new Stack<Object>();
            IdentityHashMap<Object, Object> visited = new IdentityHashMap<Object, Object>();

            if (root != null) {
                for (Object object : root) {
                    nullSafeAdd(toVisit, object);
                }
            }

            while (!toVisit.isEmpty()) {
                warned = checkMaxDepth(maxDepth, abortWhenMaxDepthExceeded, warned, visited);

                Object ref = toVisit.pop();

                if (visited.containsKey(ref)) {
                    continue;
                }

                Class<?> refClass = ref.getClass();
                if (shouldWalkClass(refClass)) {
                    if (refClass.isArray() && !refClass.getComponentType().isPrimitive()) {
                        for (int i = 0; i < Array.getLength(ref); i++) {
                            nullSafeAdd(toVisit, Array.get(ref, i));
                        }
                    } else {
                        for (Field field : getFilteredFields(refClass)) {
                            try {
                                nullSafeAdd(toVisit, field.get(ref));
                            } catch (IllegalAccessException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }

                    result += visitor.visit(ref);
                }
                visited.put(ref, null);
            }

            return result;
        } catch (MaxDepthExceededException we) {
            we.addToMeasuredSize(result);
            throw we;
        }
    }

    private boolean checkMaxDepth(final int maxDepth, final boolean abortWhenMaxDepthExceeded, boolean warned,
                                  final IdentityHashMap<Object, Object> visited) {
        if (visited.size() >= maxDepth) {
            if (abortWhenMaxDepthExceeded) {
                throw new MaxDepthExceededException(MessageFormat.format(ABORTION_MESSAGE, maxDepth));
            } else if (!warned) {
                LOG.warn(MessageFormat.format(ABORTION_MESSAGE, maxDepth));
                warned = true;
            }
        }
        return warned;
    }

    /**
     * Returns the filtered fields for a particular type
     * @param refClass the type
     * @return A collection of fields to be visited
     */
    private Collection<Field> getFilteredFields(Class<?> refClass) {
        SoftReference<Collection<Field>> ref = fieldCache.get(refClass);
        Collection<Field> fieldList = ref != null ? ref.get() : null;
        if (fieldList != null) {
            return fieldList;
        } else {
            Collection<Field> result = sizeOfFilter.filterFields(refClass, getAllFields(refClass));
            fieldCache.put(refClass, new SoftReference<Collection<Field>>(result));
            return result;
        }
    }

    private boolean shouldWalkClass(Class<?> refClass) {
        Boolean cached = classCache.get(refClass);
        if (cached == null) {
            cached = sizeOfFilter.filterClass(refClass);
            classCache.put(refClass, cached);
        }
        return cached.booleanValue();
    }
    
    private static void nullSafeAdd(final Stack<Object> toVisit, final Object o) {
        if (o != null) {
            toVisit.push(o);
        }
    }

    /**
     * Returns all non-primitive fields for the entire class hierarchy of a type
     * @param refClass the type
     * @return all fields for that type
     */
    private static Collection<Field> getAllFields(Class<?> refClass) {
        Collection<Field> fields = new ArrayList<Field>();
        for (Class<?> klazz = refClass; klazz != null; klazz = klazz.getSuperclass()) {
            for (Field field : klazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && 
                        !field.getType().isPrimitive() && 
                        !field.getName().startsWith(TC_INTERNAL_FIELD_PREFIX)) {
                    try {
                        field.setAccessible(true);
                    } catch (SecurityException e) {
                        LOG.error("Security settings prevent Ehcache from accessing the subgraph beneath '{}'" +
                                " - cache sizes may be underestimated as a result", field, e);
                        continue;
                    }
                    fields.add(field);
                }
            }
        }
        return fields;
    }
}
