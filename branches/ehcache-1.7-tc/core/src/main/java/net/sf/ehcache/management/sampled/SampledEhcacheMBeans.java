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

package net.sf.ehcache.management.sampled;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Utility class used for getting {@link ObjectName}'s for sampled MBeans
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public abstract class SampledEhcacheMBeans {

    private static final String CACHE_MANAGER_MBEAN_TYPE = "SampledCacheManagerMBean";
    private static final String CACHE_MBEAN_TYPE = "SampledCacheMBean";
    private static final String CACHE_MANAGER_QUALIFIER = "SampledCacheManager";

    /**
     * Returns an ObjectName for the passed cacheManagerName
     * 
     * @param cacheManagerName
     * @return
     * @throws MalformedObjectNameException
     */
    public static ObjectName getCacheManagerObjectName(String cacheManagerName)
            throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("net.sf.ehcache:type="
                + CACHE_MANAGER_MBEAN_TYPE + ",name=" + cacheManagerName);
        return objectName;
    }

    /**
     * Returns an ObjectName for the passed cacheManagerName, cacheName
     * combination
     * 
     * @param cacheManagerName
     * @param cacheName
     * @return
     * @throws MalformedObjectNameException
     */
    public static ObjectName getCacheObjectName(String cacheManagerName,
            String cacheName) throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("net.sf.ehcache:type="
                + CACHE_MBEAN_TYPE + "," + CACHE_MANAGER_QUALIFIER + "="
                + cacheManagerName + ",name=" + cacheName);
        return objectName;
    }

    /**
     * Returns an ObjectName that can be used for querying all Cache
     * ObjectName's for the passed cacheManagerName
     * 
     * @param cacheManagerName
     * @return
     * @throws MalformedObjectNameException
     */
    public static ObjectName getQueryObjectNameForCacheManager(
            String cacheManagerName) throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName("net.sf.ehcache:type=*,"
                + CACHE_MANAGER_QUALIFIER + "=" + cacheManagerName);
        return objectName;
    }

}
