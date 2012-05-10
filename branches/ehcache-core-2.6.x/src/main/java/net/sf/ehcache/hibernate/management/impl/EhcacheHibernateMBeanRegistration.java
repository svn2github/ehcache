/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.hibernate.management.impl;

import java.util.Properties;

import net.sf.ehcache.CacheManager;

import org.hibernate.SessionFactory;

/**
 * Interface for helping registering mbeans for ehcache backed hibernate second-level cache
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public interface EhcacheHibernateMBeanRegistration {

    /**
     * Registers MBean for the input manager and session factory properties.
     * <p />
     * MBeans will be registered based on the input session factory name. If the input name is null or blank, the name of the cache-manager
     * is used
     * 
     * @param manager
     * @param properties
     * @throws Exception
     */
    public void registerMBeanForCacheManager(CacheManager manager, Properties properties) throws Exception;

    /**
     * Enable hibernate statistics in the mbean.
     * 
     * @param sessionFactory
     */
    public void enableHibernateStatisticsSupport(SessionFactory sessionFactory);

}
