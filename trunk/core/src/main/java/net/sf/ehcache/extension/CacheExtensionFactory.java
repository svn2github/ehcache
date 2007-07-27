/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

package net.sf.ehcache.extension;

import net.sf.ehcache.Ehcache;

import java.util.Properties;

/**
 * An abstract factory for creating <code>CacheExtension</code>s. Implementers should provide their own
 * concrete factory extending this factory. It can then be configured in ehcache.xml.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public abstract class CacheExtensionFactory {

    /**
     * @param cache the cache this extension should hold a reference to, and to whose lifecycle it should be bound.
     * @param properties implementation specific properties configured as delimiter separated name value pairs in ehcache.xml
     */
    public abstract CacheExtension createCacheExtension(Ehcache cache, Properties properties); 

}
