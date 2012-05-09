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

package net.sf.ehcache.config.generator.model;

/**
 * Implementation of the {@link NodeAttribute} interface
 *
 * @author Abhishek Sanoujam
 *
 */
public class SimpleNodeAttribute implements NodeAttribute {

    private final String name;
    private String value;
    private String defaultValue;
    private boolean optional = true;

    /**
     * Constructor accepting the name of the attribute
     *
     * @param name
     *            the name of the attribute
     */
    public SimpleNodeAttribute(String name) {
        this(name, (String) null);
    }

    /**
     * Constructor accepting name and Enum value of the attribute
     *
     * @param name
     *            the name of the attribute
     * @param value
     *            the Enum value of the attribute
     */
    public SimpleNodeAttribute(String name, Enum value) {
        this(name, value.name().toLowerCase());
    }

    /**
     * Constructor accepting name and int value of the attribute
     *
     * @param name
     *            the name of the attribute
     * @param value
     *            the int value of the attribute
     */
    public SimpleNodeAttribute(String name, int value) {
        this(name, String.valueOf(value));
    }

    /**
     * Constructor accepting name and long value of the attribute
     *
     * @param name
     *            the name of the attribute
     * @param value
     *            the long value of the attribute
     */
    public SimpleNodeAttribute(String name, long value) {
        this(name, String.valueOf(value));
    }

    /**
     * Constructor accepting name and boolean value of the attribute
     *
     * @param name
     *            the name of the attribute
     * @param value
     *            the boolean value of the attribute
     */
    public SimpleNodeAttribute(String name, boolean value) {
        this(name, String.valueOf(value));
    }

    /**
     * Constructor accepting name and String value of the attribute
     *
     * @param name
     *            the name of the attribute
     * @param value
     *            the String value of the attribute
     */
    public SimpleNodeAttribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * {@inheritDoc}
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NodeAttribute)) {
            return false;
        }
        NodeAttribute other = (NodeAttribute) obj;
        if (name == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!name.equals(other.getName())) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SimpleAttribute [name=" + name + "]";
    }

    /**
     * {@inheritDoc}
     */
    public SimpleNodeAttribute optional(boolean optional) {
        this.optional = optional;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public SimpleNodeAttribute defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Same as {@link #defaultValue(String)} using String.valueOf(defaultValue)
     *
     * @param defaultValue
     *            the default value
     * @return the same instance
     */
    public SimpleNodeAttribute defaultValue(boolean defaultValue) {
        return this.defaultValue(String.valueOf(defaultValue));
    }

    /**
     * Same as {@link #defaultValue(String)} using String.valueOf(defaultValue)
     *
     * @param defaultValue
     *            the default value
     * @return the same instance
     */
    public SimpleNodeAttribute defaultValue(int defaultValue) {
        return this.defaultValue(String.valueOf(defaultValue));
    }

    /**
     * Same as {@link #defaultValue(String)} using String.valueOf(defaultValue)
     *
     * @param defaultValue
     *            the default value
     * @return the same instance
     */
    public SimpleNodeAttribute defaultValue(Enum defaultValue) {
        return this.defaultValue(defaultValue.name().toLowerCase());
    }

    /**
     * Same as {@link #defaultValue(String)} using String.valueOf(defaultValue)
     *
     * @param defaultValue
     *            the default value
     * @return the same instance
     */
    public SimpleNodeAttribute defaultValue(long defaultValue) {
        return this.defaultValue(String.valueOf(defaultValue));
    }

}
