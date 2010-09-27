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

package net.sf.ehcache.search.attribute;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.search.SearchException;

public enum AttributeType {

    BOOLEAN {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Boolean)) {
                throw new SearchException("Expecting a Boolean value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    BYTE {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Byte)) {
                throw new SearchException("Expecting a Byte value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    CHAR {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Character)) {
                throw new SearchException("Expecting a Character value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    DOUBLE {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Double)) {
                throw new SearchException("Expecting a Double value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    FLOAT {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Float)) {
                throw new SearchException("Expecting a Float value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    INT {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Integer)) {
                throw new SearchException("Expecting an Integer value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    LONG {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Long)) {
                throw new SearchException("Expecting a Long value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    SHORT {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Short)) {
                throw new SearchException("Expecting a Short value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    DATE {
        @Override
        public void validateValue(String name, Object value) {
            if (value == null || value.getClass() != Date.class) {
                throw new SearchException("Expecting a Date value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    ENUM {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof Enum)) {
                throw new SearchException("Expecting a enum value for attribute [" + name + "] but was " + type(value));
            }
        }
    },
    STRING {
        @Override
        public void validateValue(String name, Object value) {
            if (!(value instanceof String)) {
                throw new SearchException("Expecting a String value for attribute [" + name + "] but was " + type(value));
            }
        }
    };

    public static AttributeType typeFor(String name, Object value) {
        if (name == null)
            throw new NullPointerException("null name");
        if (value == null)
            throw new NullPointerException("null value");

        AttributeType type = mappings.get(value.getClass());
        if (type != null)
            return type;

        // check for enum -- calling getClass().isEnum() isn't correct in this context
        if (value instanceof Enum) {
            return ENUM;
        }

        throw new SearchException("Unsupported type for search attribute [" + name + "]: " + value.getClass().getName());
    }

    public abstract void validateValue(String name, Object value);

    private static String type(Object value) {
        if (value == null) {
            return "null";
        }
        return value.getClass().getName();
    }

    private static final Map<Class, AttributeType> mappings = new HashMap<Class, AttributeType>();

    static {
        mappings.put(Boolean.class, BOOLEAN);
        mappings.put(Byte.class, BYTE);
        mappings.put(Character.class, CHAR);
        mappings.put(Double.class, DOUBLE);
        mappings.put(Float.class, FLOAT);
        mappings.put(Integer.class, INT);
        mappings.put(Long.class, LONG);
        mappings.put(Short.class, SHORT);
        mappings.put(String.class, STRING);
        mappings.put(java.util.Date.class, DATE);
    }

}
