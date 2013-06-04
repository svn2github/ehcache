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
package net.sf.ehcache.constructs.scheduledrefresh;

import java.util.Properties;

import net.sf.ehcache.Ehcache;

import org.quartz.impl.StdSchedulerFactory;

/**
 * An example factory for creating Jdbc TX quartz job stores. Relies on proper
 * configuration elements being set in the config via the ehcache.xml config.
 * Or, a subclass of this class could easily define them from other sources.
 * 
 * @author cschanck
 */
public class ScheduledRefreshJdbcTxJobStoreFactory implements ScheduledRefreshJobStorePropertiesFactory {

   /**
    * Return the necessary job store properties to initialize a JDBC job store
    * in Quartz.
    */
   @Override
   public Properties jobStoreProperties(Ehcache underlyingCache, ScheduledRefreshConfiguration config) {
      // get the exces properties -- should have everything you need for JDBC
      Properties p = new Properties(config.getExcessProperties());
      // enforce the JDBC job store class
      p.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID);
      p.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, org.quartz.impl.jdbcjobstore.JobStoreTX.class.getName());
      return p;
   }

}
