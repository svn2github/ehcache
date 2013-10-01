/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.statistics.beans;

/**
 * The Class AttributeProxy, used to proxy operations from a dynamic mbean to a POJO object.
 * Override get()/set() as needed.
 *
 * @param <T> the return type/set type for the attribute
 *
 * @author cschanck
 */
public abstract class AttributeProxy<T> {
    private final String name;
    private final Class<T> clazz;
    private final boolean isWrite;
    private final boolean isRead;
    private final String description;

    /**
     * Instantiates a new attribute proxy.
     *
     * @param clazz the clazz of the return type
     * @param name the name
     * @param description the description
     * @param isRead readable
     * @param isWrite writable
     */
    public AttributeProxy(Class<T> clazz, String name, String description, boolean isRead, boolean isWrite) {
        this.name = name;
        this.description = description;
        this.clazz = clazz;
        this.isWrite = isWrite;
        this.isRead = isRead;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the type class.
     *
     * @return the type class
     */
    public Class<?> getTypeClass() {
        return clazz;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the value.
     *
     * @param name the name
     * @return the value
     */
    public T get(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the value.
     *
     * @param name the name
     * @param t the value
     */
    public void set(String name, T t) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if is readable.
     *
     * @return true, if is read
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Checks if is writable.
     *
     * @return true, if is writable
     */
    public boolean isWrite() {
        return isWrite;
    }

}
