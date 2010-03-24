/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.writer;

import net.sf.ehcache.Ehcache;

import java.util.Properties;

/**
 * An abstract factory for creating cache writers. Implementers should provide their own
 * concrete factory extending this factory.
 * <p/>
 * Note that Ehcache API also allows the CacheWriter to be set programmatically.
 *
 * @author Greg Luck
 * @author Geert Bevin
 * @version $Id$
 */
public abstract class CacheWriterFactory {

    /**
     * Creates a CacheWriter using the Ehcache configuration mechanism at the time the associated cache is created.
     *
     * @param cache a reference to the owning cache
     * @param properties configuration properties that will be ignored by Ehcache, but may be useful for specifying
     * the underlying resource. e.g. dataSourceName could be specified and then looked up in JNDI.
     *
     * @return a constructed CacheWriter
     */
    public abstract CacheWriter createCacheWriter(Ehcache cache, Properties properties);
}