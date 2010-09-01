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

package net.sf.ehcache.management;

import java.io.Serializable;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.hibernate.management.impl.EhcacheHibernateMbeanNames;

/**
 * Abstract super-class for Store specific dynamic mbeans.
 *
 * @author Chris Dennis
 */
public abstract class CacheStore implements Serializable {

    private ObjectName objectName;

    /**
     * Creates an mbean named for the given cache.
     */
    public CacheStore(Ehcache ehcache) {
        objectName = createObjectName(ehcache.getCacheManager().getName(), ehcache.getName());
    }

    /**
     * Creates an object name using the scheme "net.sf.ehcache:type=CacheStore,CacheManager=<cacheManagerName>,name=<cacheName>"
     */
    static ObjectName createObjectName(String cacheManagerName, String cacheName) {
        try {
            return new ObjectName("net.sf.ehcache:type=CacheStore,CacheManager=" + cacheManagerName + ",name="
                    + EhcacheHibernateMbeanNames.mbeanSafe(cacheName));
        } catch (MalformedObjectNameException e) {
            throw new CacheException(e);
        }
    }

    /**
     * @return the object name for this MBean
     */
    public ObjectName getObjectName() {
        return objectName;
    }
}
