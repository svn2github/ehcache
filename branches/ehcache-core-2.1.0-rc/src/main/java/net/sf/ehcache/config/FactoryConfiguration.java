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

package net.sf.ehcache.config;

/**
 * A class to represent the CacheManagerEventListener configuration.
 *
 * @param <T> the concrete factory type
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class FactoryConfiguration<T extends FactoryConfiguration> implements Cloneable {
    /**
     * class name.
     */
    protected String fullyQualifiedClassPath;

    /**
     * properties.
     */
    protected String properties;

    /**
     * A property separator. By default it is a comma, but other separators can be configured.
     */
    protected String propertySeparator;


    /**
     * Clones this object, following the usual contract.
     *
     * @return a copy, which independent other than configurations than cannot change.
     */
    @Override
    public T clone() {
        FactoryConfiguration config;
        try {
            config = (FactoryConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        return (T)config;
    }

    /**
     * Sets the class name.
     *
     * @param fullyQualifiedClassPath
     */
    public final void setClass(String fullyQualifiedClassPath) {
        this.fullyQualifiedClassPath = fullyQualifiedClassPath;
    }

    /**
     * @return this configuration instance
     * @see #setClass(String)
     */
    public T className(String fullyQualifiedClassPath) {
        setClass(fullyQualifiedClassPath);
        return (T)this;
    }

    /**
     * Getter.
     */
    public final String getFullyQualifiedClassPath() {
        return fullyQualifiedClassPath;
    }

    /**
     * Sets the configuration properties.
     *
     * @param properties
     */
    public final void setProperties(String properties) {
        this.properties = properties;
    }

    /**
     * @return this configuration instance
     * @see #setProperties(String)
     */
    public T properties(String properties) {
        setProperties(properties);
        return (T)this;
    }

    /**
     * Getter.
     */
    public final String getProperties() {
        return properties;
    }

    /**
     * Setter
     */
    public void setPropertySeparator(String propertySeparator) {
        this.propertySeparator = propertySeparator;
    }

    /**
     * @return this configuration instance
     * @see #setPropertySeparator(String)
     */
    public T propertySeparator(String propertySeparator) {
        setPropertySeparator(propertySeparator);
        return (T)this;
    }

    /**
     * Getter
     */
    public String getPropertySeparator() {
        return propertySeparator;
    }
}
