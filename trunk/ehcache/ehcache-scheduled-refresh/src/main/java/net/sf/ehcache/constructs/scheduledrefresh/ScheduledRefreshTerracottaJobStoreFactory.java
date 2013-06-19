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

import net.sf.ehcache.Ehcache;
import org.quartz.impl.StdSchedulerFactory;
import org.terracotta.quartz.AbstractTerracottaJobStore;
import org.terracotta.quartz.TerracottaJobStore;

import java.util.Properties;

/**
 * An example factory for creating Terracotta quartz job stores.
 * 
 * @author cschanck
 */
public class ScheduledRefreshTerracottaJobStoreFactory implements ScheduledRefreshJobStorePropertiesFactory {

   /**
    * Return the necessary job store properties to initialize a JDBC job store
    * in Quartz.
    */
   @Override
   public Properties jobStoreProperties(Ehcache underlyingCache, ScheduledRefreshConfiguration config) {
      Properties p = new Properties(config.getExcessProperties());
      p.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, TerracottaJobStore.class.getName());
      p.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID);
      p.setProperty(AbstractTerracottaJobStore.TC_CONFIGURL_PROP, config.getTerracottaConfigUrl());
      // Held off unto rejoin p.setProperty("org.quartz.jobStore.synchronousWrite", Boolean.TRUE.toString());
      // Held off until rejoin p.setProperty("org.quartz.jobStore.rejoin", Boolean.TRUE.toString());

     return p;
   }

}
