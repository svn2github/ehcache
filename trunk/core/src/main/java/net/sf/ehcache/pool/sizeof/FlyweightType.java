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

/**
 * Enum with all the flyweight types that we check for sizeOf measurements
 *
 * @author Alex Snaps
 */
enum FlyweightType {

    //XXX These checks will end up interning all objects passed in - this could be fatal for Strings
    /**
     * java.lang.Enum
     */
    ENUM(Enum.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.lang.String
     */
    STRING(String.class) {
        @Override
        boolean isShared(final Object obj) { return obj == ((String)obj).intern(); }
    },
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
        boolean isShared(final Object obj) { return obj == Integer.valueOf((Integer)obj); }
    },
    /**
     * java.lang.Short
     */
    SHORT(Short.class) {
        @Override
        boolean isShared(final Object obj) { return obj == Short.valueOf((Short)obj); }
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
        boolean isShared(final Object obj) { return obj == Long.valueOf((Long)obj); }
    },
    /**
     * java.lang.Character
     */
    CHARACTER(Character.class) {
        @Override
        boolean isShared(final Object obj) { return obj == Character.valueOf((Character)obj); }
    };

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
     * Will check whether the type of Object is a flyweight type
     * @param object the object to check for
     * @return true, if the type is flyweight
     */
    static boolean isFlyweight(Object object) {
        return object != null && getFlyweightType(object.getClass()) != null;
    }

    /**
     * Will return the Flyweight enum instance for the flyweight Class, or null if type isn't flyweight
     * @param aClazz the class we need the FlyweightType instance for
     * @return the FlyweightType, or null
     */
    static FlyweightType getFlyweightType(final Class<?> aClazz) {
        for (FlyweightType flyweightType : values()) {
            if (flyweightType.clazz == aClazz) {
                return flyweightType;
            }
        }
        return null;
    }
}
