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
package net.sf.ehcache.hibernate.management;

/**
 * MBean interface for hibernate statistics of session-factories
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public interface EhcacheHibernateMBean extends EhcacheStats, HibernateStats {
    // public static final String CACHE_ENABLED = "CacheEnabled";
    // public static final String CACHE_REGION_CHANGED = "CacheRegionChanged";
    // public static final String CACHE_FLUSHED = "CacheFlushed";
    // public static final String CACHE_REGION_FLUSHED = "CacheRegionFlushed";
    // public static final String CACHE_STATISTICS_ENABLED = "CacheStatisticsEnabled";
    // public static final String CACHE_STATISTICS_RESET = "CacheStatisticsReset";

    /**
     * Returns true if hibernate statistics is supported.
     * <p />
     * It depends on whether the property <tt>hibernate.session_factory_name</tt> has been specified in the hibernate configuration for the
     * hibernate statistics to be enabled
     * 
     * @return true if hibernate statistics is supported
     */
    public boolean isHibernateStatisticsSupported();
}
