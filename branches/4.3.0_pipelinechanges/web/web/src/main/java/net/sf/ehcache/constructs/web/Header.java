/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.constructs.web;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic implementation of a HTTP header. Handles String, Int and Date typed headers.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 * @param <T> The type of Header value being stored. Must implement {@link Serializable}
 */
public class Header<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Used to help differentiate the different header types
     */
    public enum Type {
        /**
         * A String Header. {@link javax.servlet.http.HttpServletResponse#setHeader(String, String)}
         */
        STRING(String.class),
        /**
         * A date Header. {@link javax.servlet.http.HttpServletResponse#setDateHeader(String, long)}
         */
        DATE(Long.class),
        /**
         * A int Header. {@link javax.servlet.http.HttpServletResponse#setIntHeader(String, int)}
         */
        INT(Integer.class);
        
        private static final Map<Class<? extends Serializable>, Type> TYPE_LOOKUP = 
            new ConcurrentHashMap<Class<? extends Serializable>, Type>();
        private final Class<? extends Serializable> type;
        
        /**
         * Create a new Type
         */
        private Type(Class<? extends Serializable> type) {
            this.type = type;
        }

        /**
         * @return The header type class this Type represents
         */
        public Class<? extends Serializable> getTypeClass() {
            return this.type;
        }
        
        /**
         * Determines the {@link Type} of the Header. Throws IllegalArgumentException if the specified class does not match any of the Types
         */
        public static Type determineType(Class<? extends Serializable> typeClass) {
            final Type lookupType = TYPE_LOOKUP.get(typeClass);
            if (lookupType != null) {
                return lookupType;
            }
            
            for (final Type t : Type.values()) {
                if (typeClass == t.getTypeClass()) {
                    //If the class explicitly matches add to the lookup cache
                    TYPE_LOOKUP.put(typeClass, t);
                    return t;
                }
                
                if (typeClass.isAssignableFrom(t.getTypeClass())) {
                    return t;
                }
            }
            
            throw new IllegalArgumentException("No Type for class " + typeClass);
        }
    }

    private final String name;
    private final T value;
    private final Type type;

    /**
     * Create a new Header
     * 
     * @param name Name of the header, may not be null
     * @param value Value of the header, may not be null
     */
    public Header(String name, T value) {
        if (name == null) {
            throw new IllegalArgumentException("Header cannnot have a null name");
        }
        if (value == null) {
            throw new IllegalArgumentException("Header cannnot have a null value");
        }
        this.name = name;
        this.value = value;
        this.type = Type.determineType(value.getClass());
    }

    /**
     * @return Name of the header, will never be null
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return Value for the header, will never be null
     */
    public T getValue() {
        return this.value;
    }
    
    /**
     * @return The header type
     */
    public Type getType() {
        return this.type;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Header<?> other = (Header<?>) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!this.type.equals(other.type)) {
            return false;
        }
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Header<" + this.type.getTypeClass().getSimpleName() + "> [name=" + this.name + ", value=" + this.value + "]";
    }
}
