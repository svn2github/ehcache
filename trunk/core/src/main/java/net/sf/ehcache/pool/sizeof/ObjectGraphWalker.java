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
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;

/**
 * This will walk an object graph and let you execute some "function" along the way
 * @author Alex Snaps
 */
final class ObjectGraphWalker {

    // Todo this is probably not what we want...
    private final ConcurrentMap<Class<?>, SoftReference<Collection<Field>>> fieldCache =
            new ConcurrentHashMap<Class<?>, SoftReference<Collection<Field>>>();
    private final ConcurrentMap<Class<?>, Boolean> classCache =
            new ConcurrentHashMap<Class<?>, Boolean>();

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
     * @param root the roots of the objects (a shared graph will only be visited once)
     * @return the sum of all Visitor#visit returned values
     */
    long walk(Object... root) {

        long result = 0;

        Stack<Object> toVisit = new Stack<Object>();
        IdentityHashMap<Object, Object> visited = new IdentityHashMap<Object, Object>();

        if (root != null) {
            for (Object object : root) {
                nullSafeAdd(toVisit, object);
            }
        }

        while (!toVisit.isEmpty()) {

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
                if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
        }
        return fields;
    }
}
