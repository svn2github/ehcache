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

import net.sf.ehcache.pool.sizeof.filter.PassThroughFilter;
import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;

import sun.misc.Unsafe;

import static net.sf.ehcache.pool.sizeof.JvmInformation.MINIMUM_OBJECT_SIZE;
import static net.sf.ehcache.pool.sizeof.JvmInformation.OBJECT_ALIGNMENT;

/**
 * {@link sun.misc.Unsafe#theUnsafe} based sizeOf measurement
 * All constructors will throw UnsupportedOperationException if theUnsafe isn't accessible on this platform
 * @author Chris Dennis
 */
@SuppressWarnings("restriction")
public class UnsafeSizeOf extends SizeOf {

    private static final Unsafe UNSAFE;

    static {
        Unsafe unsafe;
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe)unsafeField.get(null);
        } catch (Throwable t) {
            unsafe = null;
        }
        UNSAFE = unsafe;
    }

    /**
     * Builds a new SizeOf that will not filter fields and will cache reflected fields
     *
     * @throws UnsupportedOperationException If Unsafe isn't accessible
     * @see #UnsafeSizeOf(net.sf.ehcache.pool.sizeof.filter.SizeOfFilter, boolean)
     */
    public UnsafeSizeOf() throws UnsupportedOperationException {
        this(new PassThroughFilter());
    }

    /**
     * Builds a new SizeOf that will filter fields according to the provided filter and will cache reflected fields
     *
     * @param filter The filter to apply
     * @throws UnsupportedOperationException If Unsafe isn't accessible
     * @see #UnsafeSizeOf(net.sf.ehcache.pool.sizeof.filter.SizeOfFilter, boolean)
     * @see SizeOfFilter
     */
    public UnsafeSizeOf(SizeOfFilter filter) throws UnsupportedOperationException {
        this(filter, true);
    }

    /**
     * Builds a new SizeOf that will filter fields according to the provided filter
     *
     * @param filter The filter to apply
     * @param caching     whether to cache reflected fields
     * @throws UnsupportedOperationException If Unsafe isn't accessible
     * @see SizeOfFilter
     */
    public UnsafeSizeOf(SizeOfFilter filter, boolean caching) throws UnsupportedOperationException {
        super(filter, caching);
        if (UNSAFE == null) {
            throw new UnsupportedOperationException("sun.misc.Unsafe instance not accessible");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long measureSizeOf(Object obj) {
        if (obj.getClass().isArray()) {
            Class<?> klazz = obj.getClass();
            int base = UNSAFE.arrayBaseOffset(klazz);
            int scale = UNSAFE.arrayIndexScale(klazz);
            long size = base + (scale * Array.getLength(obj));
            if ((size % OBJECT_ALIGNMENT) != 0) {
                size += OBJECT_ALIGNMENT - (size % OBJECT_ALIGNMENT);
            }
            return Math.max(MINIMUM_OBJECT_SIZE, size);
        } else {
            for (Class<?> klazz = obj.getClass(); klazz != null; klazz = klazz.getSuperclass()) {
                long lastFieldOffset = -1;
                for (Field f : klazz.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        lastFieldOffset = Math.max(lastFieldOffset, UNSAFE.objectFieldOffset(f));
                    }
                }
                if (lastFieldOffset > 0) {
                    lastFieldOffset += 1;
                    if ((lastFieldOffset % OBJECT_ALIGNMENT) != 0) {
                        lastFieldOffset += OBJECT_ALIGNMENT - (lastFieldOffset % OBJECT_ALIGNMENT);
                    }
                    return Math.max(MINIMUM_OBJECT_SIZE, lastFieldOffset);
                }
            }

            long size = PrimitiveType.CLASS.getSize();
            if ((size % OBJECT_ALIGNMENT) != 0) {
                size += OBJECT_ALIGNMENT - (size % OBJECT_ALIGNMENT);
            }
            return Math.max(MINIMUM_OBJECT_SIZE, size);
        }
    }

}
