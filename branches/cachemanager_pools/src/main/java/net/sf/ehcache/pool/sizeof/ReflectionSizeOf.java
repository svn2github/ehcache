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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Stack;

import net.sf.ehcache.pool.sizeof.filter.PassThroughFilter;
import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;

import static net.sf.ehcache.pool.sizeof.JvmInformation.MINIMUM_OBJECT_SIZE;
import static net.sf.ehcache.pool.sizeof.JvmInformation.OBJECT_ALIGNMENT;
import static net.sf.ehcache.pool.sizeof.JvmInformation.POINTER_SIZE;

/**
 * SizeOf that uses reflection to measure on heap size of object graphs
 *
 * @author Alex Snaps
 * @author Chris Dennis
 */
public class ReflectionSizeOf extends SizeOf {

    /**
     * Builds a new SizeOf that will not filter fields and will cache reflected fields
     * @see #ReflectionSizeOf(net.sf.ehcache.pool.sizeof.filter.SizeOfFilter, boolean)
     */
    public ReflectionSizeOf() {
        this(new PassThroughFilter());
    }

    /**
     * Builds a new SizeOf that will filter fields and will cache reflected fields
     * @param fieldFilter The filter to apply
     * @see #ReflectionSizeOf(net.sf.ehcache.pool.sizeof.filter.SizeOfFilter, boolean)
     * @see SizeOfFilter
     */
    public ReflectionSizeOf(SizeOfFilter fieldFilter) {
        this(fieldFilter, true);
    }

    /**
     * Builds a new SizeOf that will filter fields
     * @param fieldFilter The filter to apply
     * @param caching Whether to cache reflected fields
     * @see SizeOfFilter
     */
    public ReflectionSizeOf(SizeOfFilter fieldFilter, boolean caching) {
        super(fieldFilter, caching);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long measureSizeOf(Object obj) {
        if (obj == null) {
            return 0;
        }

        Class<?> aClass = obj.getClass();
        if (aClass.isArray()) {
            return guessArraySize(obj);
        } else {
            long size = PrimitiveType.CLASS.getSize();

            Stack<Class<?>> classStack = new Stack<Class<?>>();
            for (Class<?> klazz = aClass; klazz != null; klazz = klazz.getSuperclass()) {
                classStack.push(klazz);
            }

            while (!classStack.isEmpty()) {
                Class<?> klazz = classStack.pop();

                //assuming default class layout
                int oops = 0;
                int doubles = 0;
                int words = 0;
                int shorts = 0;
                int bytes = 0;
                for (Field f : klazz.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    if (f.getType().isPrimitive()) {
                        switch (PrimitiveType.forType(f.getType())) {
                            case BOOLEAN:
                            case BYTE:
                                bytes++;
                                break;
                            case SHORT:
                            case CHAR:
                                shorts++;
                                break;
                            case INT:
                            case FLOAT:
                                words++;
                                break;
                            case DOUBLE:
                            case LONG:
                                doubles++;
                                break;
                            default:
                                throw new AssertionError();
                        }
                    } else {
                        oops++;
                    }
                }
                if (doubles > 0 && (size % PrimitiveType.LONG.getSize()) != 0) {
                    long length = PrimitiveType.LONG.getSize() - (size % PrimitiveType.LONG.getSize());
                    size += PrimitiveType.LONG.getSize() - (size % PrimitiveType.LONG.getSize());

                    while (length >= PrimitiveType.INT.getSize() && words > 0) {
                        length -= PrimitiveType.INT.getSize();
                        words--;
                    }
                    while (length >= PrimitiveType.SHORT.getSize() && shorts > 0) {
                        length -= PrimitiveType.SHORT.getSize();
                        shorts--;
                    }
                    while (length >= PrimitiveType.BYTE.getSize() && bytes > 0) {
                        length -= PrimitiveType.BYTE.getSize();
                        bytes--;
                    }
                    while (length >= PrimitiveType.getReferenceSize() && oops > 0) {
                        length -= PrimitiveType.getReferenceSize();
                        oops--;
                    }
                }
                size += PrimitiveType.DOUBLE.getSize() * doubles;
                size += PrimitiveType.INT.getSize() * words;
                size += PrimitiveType.SHORT.getSize() * shorts;
                size += PrimitiveType.BYTE.getSize() * bytes;

                if (oops > 0) {
                    if ((size % PrimitiveType.getReferenceSize()) != 0) {
                        size += PrimitiveType.getReferenceSize() - (size % PrimitiveType.getReferenceSize());
                    }
                    size += oops * PrimitiveType.getReferenceSize();
                }

                if ((doubles + words + shorts + bytes + oops) > 0 && (size % POINTER_SIZE) != 0) {
                    size += JvmInformation.POINTER_SIZE - (size % POINTER_SIZE);
                }
            }
            if ((size % OBJECT_ALIGNMENT) != 0) {
                size += OBJECT_ALIGNMENT - (size % OBJECT_ALIGNMENT);
            }
            return Math.max(size, MINIMUM_OBJECT_SIZE);
        }
    }

    private long guessArraySize(Object obj) {
        long size = PrimitiveType.getArraySize();
        int length = Array.getLength(obj);
        if (length != 0) {
            Class<?> arrayElementClazz = obj.getClass().getComponentType();
            if (arrayElementClazz.isPrimitive()) {
                size += length * PrimitiveType.forType(arrayElementClazz).getSize();
            } else {
                size += length * PrimitiveType.getReferenceSize();
            }
        }
        if ((size % OBJECT_ALIGNMENT) != 0) {
            size += OBJECT_ALIGNMENT - (size % OBJECT_ALIGNMENT);
        }
        return Math.max(size, MINIMUM_OBJECT_SIZE);
    }
}
