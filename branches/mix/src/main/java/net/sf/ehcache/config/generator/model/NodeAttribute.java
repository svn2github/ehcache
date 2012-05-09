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
 * Interface that abstracts the idea of an attribute. An attribute has a name, a value, boolean indicating if its an optional attribute and
 * a default value
 * 
 * @author Abhishek Sanoujam
 * 
 */
public interface NodeAttribute {

    /**
     * Name of the attribute
     * 
     * @return Name of the attribute
     */
    String getName();

    /**
     * Value of the attribute
     * 
     * @return value of the attribute
     */
    String getValue();

    /**
     * Returns true if the attribute is optional, otherwise false
     * 
     * @return Returns true if the attribute is optional, otherwise false
     */
    boolean isOptional();

    /**
     * Returns the default value of the attribute
     * 
     * @return default value of the attribute
     */
    String getDefaultValue();

    /**
     * Sets this attribute to optional or not
     * 
     * @param optional
     *            true if this attribute is optional
     */
    public void setOptional(boolean optional);

    /**
     * Default value setter
     * 
     * @param defaultValue
     *            the default value
     */
    public void setDefaultValue(String defaultValue);
    
    /**
     * Setter for value
     * 
     * @param value
     *            the new value
     */
    public void setValue(String value);

    /**
     * Builder convenience method for setting optional
     * 
     * @param optional
     *            true if optional
     * @return the same attribute instance
     */
    NodeAttribute optional(boolean optional);

    /**
     * Builder convenience method for setting defaultValue
     * 
     * @param defaultValue
     *            the default value
     * @return the same attribute instance
     */
    NodeAttribute defaultValue(String defaultValue);

}
