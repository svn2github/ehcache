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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Enum with all the flyweight types that we check for sizeOf measurements
 *
 * @author Alex Snaps
 */
enum FlyweightType {

    /**
     * java.lang.Enum
     */
    ENUM(Enum.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    // XXX There is no nullipotent way of determining the interned status of a string
    /**
     * java.lang.String
     */
    //STRING(String.class) {
    //    @Override
    //    boolean isShared(final Object obj) { return obj == ((String)obj).intern(); }
    //},
    /**
     * java.lang.Boolean
     */
    BOOLEAN(Boolean.class) {
        @Override
        boolean isShared(final Object obj) { return obj == Boolean.TRUE || obj == Boolean.FALSE; }
    },
    /**
     * java.lang.Integer
     */
    INTEGER(Integer.class) {
        @Override
        boolean isShared(final Object obj) {
            int value = ((Integer)obj).intValue();
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE && obj == Integer.valueOf(value);
        }
    },
    /**
     * java.lang.Short
     */
    SHORT(Short.class) {
        @Override
        boolean isShared(final Object obj) {
            short value = ((Short)obj).shortValue();
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE && obj == Short.valueOf(value);
        }
    },
    /**
     * java.lang.Byte
     */
    BYTE(Byte.class) {
        @Override
        boolean isShared(final Object obj) { return obj == Byte.valueOf((Byte)obj); }
    },
    /**
     * java.lang.Long
     */
    LONG(Long.class) {
        @Override
        boolean isShared(final Object obj) {
            long value = ((Long)obj).longValue();
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE && obj == Long.valueOf(value);
        }
    },
    /**
     * java.lang.Character
     */
    CHARACTER(Character.class) {
        @Override
        boolean isShared(final Object obj) { return ((Character)obj).charValue() <= Byte.MAX_VALUE && obj == Character.valueOf((Character)obj); }
    },
    /**
     *  java.lang.Locale
     */
    LOCALE(Locale.class) {

        @Override
        boolean isShared(final Object obj) {
            return GLOBAL_LOCALES.contains(obj);
        }
    };

    private static final Map<Class<?>, FlyweightType> TYPE_MAPPINGS = new HashMap<Class<?>, FlyweightType>();
    static {
        for (FlyweightType type : FlyweightType.values()) {
          TYPE_MAPPINGS.put(type.clazz, type);
        }
    }

    private static final Set<Locale> GLOBAL_LOCALES;
    static {
        Map<Locale, Void> locales = new IdentityHashMap<Locale, Void>();
        for (Field f : Locale.class.getFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Locale.class.equals(f.getType())) {
                try {
                    locales.put((Locale) f.get(null), null);
                } catch (IllegalArgumentException e) {
                    continue;
                } catch (IllegalAccessException e) {
                    continue;
                }
            }
        }
        GLOBAL_LOCALES = locales.keySet();
    }

    private final Class<?> clazz;

    private FlyweightType(final Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * Whether this is a shared object
     * @param obj the object to check for
     * @return true, if shared
     */
    abstract boolean isShared(Object obj);

    /**
     * Will return the Flyweight enum instance for the flyweight Class, or null if type isn't flyweight
     * @param aClazz the class we need the FlyweightType instance for
     * @return the FlyweightType, or null
     */
    static FlyweightType getFlyweightType(final Class<?> aClazz) {
        if (aClazz.isEnum()) {
            return ENUM;
        } else {
            return TYPE_MAPPINGS.get(aClazz);
        }
    }
}
