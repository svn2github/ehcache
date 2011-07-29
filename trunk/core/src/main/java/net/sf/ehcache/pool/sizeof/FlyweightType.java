/**
 *  Copyright 2003-2011 Terracotta, Inc.
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
            return value >= -128 && value <= 127 && obj == Integer.valueOf(value);
        }
    },
    /**
     * java.lang.Short
     */
    SHORT(Short.class) {
        @Override
        boolean isShared(final Object obj) {
            short value = ((Short)obj).shortValue();
            return value >= -128 && value <= 127 && obj == Short.valueOf(value);
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
            return value >= -128 && value <= 127 && obj == Long.valueOf(value);
        }
    },
    /**
     * java.lang.Character
     */
    CHARACTER(Character.class) {
        @Override
        boolean isShared(final Object obj) { return ((Character)obj).charValue() <= 127 && obj == Character.valueOf((Character)obj); }
    },
    /**
     * java.lang.Locale
     */
    LOCALE(Locale.class) {
        @Override
        boolean isShared(final Object obj) { return /*obj == Locale.ROOT ||*//*Java 6*/
            obj == Locale.ENGLISH  || obj == Locale.FRENCH || obj == Locale.GERMAN || obj == Locale.ITALIAN ||
            obj == Locale.JAPANESE || obj == Locale.KOREAN || obj == Locale.CHINESE ||
            obj == Locale.SIMPLIFIED_CHINESE || obj == Locale.TRADITIONAL_CHINESE  || obj == Locale.FRANCE ||
            obj == Locale.GERMANY  || obj == Locale.ITALY || obj == Locale.JAPAN   ||
            obj == Locale.KOREA    || obj == Locale.CHINA || obj == Locale.PRC     || obj == Locale.TAIWAN ||
            obj == Locale.UK       || obj == Locale.US    || obj == Locale.CANADA  || obj == Locale.CANADA_FRENCH;
        }
    };

    private static final Map<Class<?>, FlyweightType> TYPE_MAPPINGS = new HashMap<Class<?>, FlyweightType>();
    static {
        for (FlyweightType type : FlyweightType.values()) {
          TYPE_MAPPINGS.put(type.clazz, type);
        }
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
