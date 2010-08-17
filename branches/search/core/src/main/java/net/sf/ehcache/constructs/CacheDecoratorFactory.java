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

package net.sf.ehcache.constructs;

import java.util.Properties;

import net.sf.ehcache.Ehcache;

/**
 * An abstract factory for creating decorated Ehcache instances. Implementing classes should provide their own
 * concrete factory extending this factory.
 * 
 * @author Abhishek Sanoujam
 */
public abstract class CacheDecoratorFactory {

    /**
     * Creates a decorated {@link Ehcache} using the properties specified for configuring the decorator.
     * 
     * @param cache
     *            a reference to the owning cache
     * @param properties
     *            implementation specific properties configured as delimiter
     *            separated name value pairs in ehcache.xml
     * @return a decorated Ehcache
     */
    public abstract Ehcache createDecoratedEhcache(Ehcache cache, Properties properties);

}