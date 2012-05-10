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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.generator.model.elements.FactoryConfigurationElement;

/**
 * An abstract implementation of {@link NodeElement}. Overrides {@link #equals(Object)} and {@link #hashCode()} methods by comparing the fully
 * qualified name of this element -- {@link #getFQName()}
 *
 * @author Abhishek Sanoujam
 *
 */
public abstract class AbstractNodeElement implements NodeElement {

    /**
     * List of attributes
     */
    protected final List<NodeAttribute> attributes = new ArrayList<NodeAttribute>();

    /**
     * List of child elements
     */
    protected final List<NodeElement> children = new ArrayList<NodeElement>();

    /**
     * The parent
     */
    protected NodeElement parent;

    /**
     * Whether this element is optional
     */
    protected boolean optional;

    /**
     * the inner string content
     */
    protected String innerContent;

    /**
     * Constructor accepting the parent of this element
     *
     * @param parent
     */
    public AbstractNodeElement(NodeElement parent) {
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    public NodeElement getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    public List<NodeAttribute> getAttributes() {
        return attributes;
    }

    /**
     * {@inheritDoc}
     */
    public List<NodeElement> getChildElements() {
        return children;
    }

    /**
     * {@inheritDoc}
     */
    public void addAttribute(NodeAttribute attribute) {
        if (attribute == null) {
            return;
        }
        this.attributes.add(attribute);
    }

    /**
     * {@inheritDoc}
     */
    public void addChildElement(NodeElement childElement) {
        if (childElement == null) {
            return;
        }
        this.children.add(childElement);
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
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public String getInnerContent() {
        return innerContent;
    }

    /**
     * {@inheritDoc}
     */
    public void setInnerContent(String content) {
        this.innerContent = content;
    }

    /**
     * Helper method that adds all the {@link FactoryConfiguration} from the parameter as child elements by creating
     * {@link FactoryConfigurationElement} for each of them
     *
     * @param element
     *            the element in which the child elements will be added
     * @param name
     *            name to be used for the child element(s)
     * @param factoryConfigurations
     *            the {@link FactoryConfiguration}'s
     */
    public static void addAllFactoryConfigsAsChildElements(NodeElement element, String name,
            Collection<? extends FactoryConfiguration> factoryConfigurations) {
        if (factoryConfigurations == null || factoryConfigurations.size() == 0) {
            return;
        }
        for (NodeElement child : getAllFactoryElements(element, name, factoryConfigurations)) {
            element.addChildElement(child);
        }
    }

    /**
     * Helper method that creates {@link FactoryConfigurationElement}'s from a collection of {@link FactoryConfiguration}'s
     *
     * @param parent
     *            the parent for each of the create {@link FactoryConfigurationElement}
     * @param name
     *            name of the element(s)
     * @param factoryConfigurations
     *            the {@link FactoryConfiguration}'s
     * @return list of {@link FactoryConfigurationElement}
     */
    public static List<FactoryConfigurationElement> getAllFactoryElements(NodeElement parent, String name,
            Collection<? extends FactoryConfiguration> factoryConfigurations) {
        List<FactoryConfigurationElement> elements = new ArrayList<FactoryConfigurationElement>();
        for (FactoryConfiguration config : factoryConfigurations) {
            elements.add(new FactoryConfigurationElement(parent, name, config));
        }
        return elements;
    }

    /**
     * {@inheritDoc}
     */
    public String getFQName() {
        return getFQName(this, ".");
    }

    /**
     * {@inheritDoc}
     */
    public String getFQName(String delimiter) {
        return getFQName(this, delimiter);
    }

    private static String getFQName(NodeElement element, String delimiter) {
        LinkedList<NodeElement> hierarchy = new LinkedList<NodeElement>();
        NodeElement curr = element;
        while (curr != null) {
            hierarchy.addFirst(curr);
            curr = curr.getParent();
        }
        StringBuilder sb = new StringBuilder();
        while (!hierarchy.isEmpty()) {
            sb.append(hierarchy.removeFirst().getName());
            if (!hierarchy.isEmpty()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFQName() == null) ? 0 : getFQName().hashCode());
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
        if (!(obj instanceof NodeElement)) {
            return false;
        }
        NodeElement other = (NodeElement) obj;
        if (getFQName() == null) {
            if (other.getFQName() != null) {
                return false;
            }
        } else if (!getFQName().equals(other.getFQName())) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AbstractElement [FQName=" + getFQName() + "]";
    }

    /**
     * {@inheritDoc}
     */
    public void accept(NodeElementVisitor visitor) {
        visitor.visit(this);
    }

}
