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

import java.util.List;

/**
 * Interface that abstracts the idea of an element. An element has a name, list of {@link NodeAttribute}'s, a parent element and child elements
 * 
 * @author Abhishek Sanoujam
 * 
 */
public interface NodeElement {

    /**
     * The name of the element
     * 
     * @return Name of the element
     */
    String getName();

    /**
     * Same as calling {@link #getFQName(String)} with the string "."
     * 
     * @return the fully qualified name of this element
     */
    String getFQName();

    /**
     * The fully qualified name of the element. The fully qualified name of the name is the name from the root element till this element
     * separated by the <code>delimiter</code> string
     * 
     * @param delimiter
     * @return the fully qualified name of this element separated by delimiter
     */
    String getFQName(String delimiter);

    /**
     * List of attributes of this element
     * 
     * @return list of attributes of this element
     */
    List<NodeAttribute> getAttributes();

    /**
     * Returns the parent of this element. May be null.
     * 
     * @return parent of this element. May be null.
     */
    NodeElement getParent();

    /**
     * Returns the list of child elements.
     * 
     * @return the list of child elements
     */
    List<NodeElement> getChildElements();

    /**
     * Returns true if there is at least one child
     * 
     * @return true if there is at least one child, otherwise false
     */
    boolean hasChildren();

    /**
     * The inner content of this element as string. Does not include the child elements
     * 
     * @return inner content of this element as string. This does not include the child elements
     */
    String getInnerContent();

    /**
     * Add an attribute
     * 
     * @param attribute
     *            add an attribute
     */
    void addAttribute(NodeAttribute attribute);

    /**
     * Adds a child element.
     * 
     * @param childElement
     *            adds a child element
     */
    void addChildElement(NodeElement childElement);

    /**
     * Accepts an {@link NodeElementVisitor}
     * 
     * @param visitor
     *            the visitor whose visit methods will be called
     */
    void accept(NodeElementVisitor visitor);

    /**
     * Returns true if this element is optional
     * 
     * @return true if this element is optional
     */
    boolean isOptional();

    /**
     * Sets optional or not
     * 
     * @param optional
     */
    void setOptional(boolean optional);

    /**
     * Sets the inner content of this element
     * 
     * @param content
     */
    void setInnerContent(String content);
}
