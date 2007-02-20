/**
 *  Copyright 2003-2007 Greg Luck
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

package net.sf.ehcache.bootstrap;

import java.util.Properties;

/**
 * An abstract factory for creating BootstrapCacheLoader instances. Implementers should provide their own
 * concrete factory extending this factory. It can then be configured in ehcache.xml.
 *
 * @author Greg Luck
 * @version $Id$
 */
public abstract class BootstrapCacheLoaderFactory {

    /**
     * Create a <code>BootstrapCacheLoader</code>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * @return a constructed BootstrapCacheLoader
     */
    public abstract BootstrapCacheLoader createBootstrapCacheLoader(Properties properties);

}
