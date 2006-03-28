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

package net.sf.ehcache.hibernate;

import net.sf.hibernate.cache.Cache;
import net.sf.hibernate.cache.CacheException;
import net.sf.hibernate.cache.CacheProvider;
import net.sf.hibernate.cache.Timestamper;

import java.util.Properties;

/**
 * Cache Provider plugin for Hibernate
 *
 * Use <code>hibernate.cache.provider_class=net.sf.ehcache.hibernate.Provider</code>
 * in Hibernate 2.1 or later.
 *
 * @version $Id: Provider.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 * @author Greg Luck
 */
public class Provider implements CacheProvider {


    /**
     * Builds a Cache.
     * <p>
     * Even though this method provides properties, they are not used.
     * Properties for EHCache are specified in the ehcache.xml file.
     * Configuration will be read from ehcache.xml for a cache declaration
     * where the name attribute matches the name parameter in this builder.
     * @param name the name of the cache. Must match a cache configured in ehcache.xml
     * @param properties not used
     * @return a newly built cache will be built and initialised
     * @throws CacheException inter alia, if a cache of the same name already exists
     * @deprecated Use net.sf.hibernate.cache.EhCacheProvider instead of this class.
     */
    public Cache buildCache(String name, Properties properties) throws CacheException {
        return new Plugin(name);
    }

    /**
     * Returns the next timestamp.
     */
    public long nextTimestamp() {
        return Timestamper.next();
    }
}
