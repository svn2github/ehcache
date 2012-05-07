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

package net.sf.ehcache.config;

import java.util.Properties;

import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeAttribute;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.JavaBeanAttributeExtractor;
import net.sf.ehcache.search.attribute.ReflectionAttributeExtractor;
import net.sf.ehcache.util.ClassLoaderUtil;
import net.sf.ehcache.util.PropertyUtil;

/**
 * A cache search attribute. Search attributes must have a name and optionally an expression or class set (if neither is set then this
 * implies java bean style)
 *
 * @author teck
 */
public class SearchAttribute {

    private String name;
    private String className;
    private String expression;
    private String properties;
    private String propertySeparator;

    /**
     * Set the attribute name
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the extractor class for this attribute. This class must be available at runtime and must implement {@link AttributeExtractor}
     *
     * @param className
     */
    public void setClass(String className) {
        if (expression != null) {
            throw new InvalidConfigurationException("Cannot set both class and expression for a search attribute");
        }
        this.className = className;
    }

    /**
     * Set the attribute expression. See {@link ReflectionAttributeExtractor} for more information
     *
     * @param expression
     */
    public void setExpression(String expression) {
        if (className != null) {
            throw new InvalidConfigurationException("Cannot set both class and expression for a search attribute");
        }
        this.expression = expression;
    }

    /**
     * Get the extractor class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Get the attribute expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Get the attribute name
     */
    public String getName() {
        return name;
    }

    /**
     * Construct the extractor for this attribute configuration
     */
    public AttributeExtractor constructExtractor() {
        if (name == null) {
            throw new InvalidConfigurationException("search attribute has no name");
        }

        if (expression != null) {
            return new ReflectionAttributeExtractor(expression);
        } else if (className != null) {
            if (properties != null) {
                return (AttributeExtractor) ClassLoaderUtil.createNewInstance(className, new Class[] {Properties.class},
                        new Object[] {PropertyUtil.parseProperties(properties, propertySeparator)});
            } else {
                return (AttributeExtractor) ClassLoaderUtil.createNewInstance(className);
            }
        } else {
            return new JavaBeanAttributeExtractor(name);
        }
    }

    /**
     * Set the attribute name
     *
     * @param name
     * @return this
     */
    public SearchAttribute name(String name) {
        setName(name);
        return this;
    }

    /**
     * Set the attribute extractor class name
     *
     * @param className
     *            attribute extractor class
     * @return this
     */
    public SearchAttribute className(String className) {
        setClass(className);
        return this;
    }

    /**
     * Set the attribute expression
     *
     * @param expression
     *            attribute expression
     * @return this
     */
    public SearchAttribute expression(String expression) {
        setExpression(expression);
        return this;
    }

    /**
     * Set the extractor properties
     *
     * @param props
     */
    public void setProperties(String props) {
        this.properties = props;
    }

    /**
     * Set the extractor properties separator
     *
     * @param sep
     */
    public void setPropertySeparator(String sep) {
        this.propertySeparator = sep;
    }

    /**
     * Set the extractor properties separator
     *
     * @param sep
     * @return this
     */
    public SearchAttribute propertySeparator(String sep) {
        setPropertySeparator(sep);
        return this;
    }

    /**
     * Set the extractor properties
     *
     * @param props
     * @return this
     */
    public SearchAttribute properties(String props) {
        setProperties(props);
        return this;
    }

    /**
     * Create a generated config element node for this search attribute definition
     *
     * @param parent the enclosing parent config element
     * @return generated config element for this search attribute
     */
    public NodeElement asConfigElement(NodeElement parent) {
        SimpleNodeElement rv = new SimpleNodeElement(parent, "searchAttribute");

        rv.addAttribute(new SimpleNodeAttribute("name", name));

        if (expression != null) {
            rv.addAttribute(new SimpleNodeAttribute("expression", expression));
        } else if (className != null) {
            rv.addAttribute(new SimpleNodeAttribute("class", className));
            if (properties != null) {
                rv.addAttribute(new SimpleNodeAttribute("properties", properties));
            }
            if (propertySeparator != null) {
                rv.addAttribute(new SimpleNodeAttribute("propertySeperator", propertySeparator));
            }
        }

        return rv;
    }

}
