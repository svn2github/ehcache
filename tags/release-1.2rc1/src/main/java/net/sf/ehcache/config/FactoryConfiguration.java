/**
 *  Copyright 2003-2006 Greg Luck
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
 * A class to represent the CacheManagerEventListener configuration
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class FactoryConfiguration {
    /**
     * class name
     */
    protected String fullyQualifiedClassPath;

    /**
     * properties
     */
    protected String properties;


    /**
     * Sets the class name
     *
     * @param fullyQualifiedClassPath
     */
    public void setClass(String fullyQualifiedClassPath) {
        this.fullyQualifiedClassPath = fullyQualifiedClassPath;
    }

    /**
     * Sets the configuration properties
     *
     * @param properties
     */
    public void setProperties(String properties) {
        this.properties = properties;
    }

    /**
     * Getter
     */
    public String getFullyQualifiedClassPath() {
        return fullyQualifiedClassPath;
    }

    /**
     * Getter
     */
    public String getProperties() {
        return properties;
    }

}
