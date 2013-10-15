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
import org.quartz.simpl.RAMJobStore;

/**
 * A factory for creating RAM job stores.
 * 
 * @author cschanck
 */
public class ScheduledRefreshRAMJobStoreFactory implements ScheduledRefreshJobStorePropertiesFactory {

   /**
    * Return the necessary job store proprties to initialize a RAM job store in
    * Quartz.
    */
   @Override
   public Properties jobStoreProperties(Ehcache underlyingCache, ScheduledRefreshConfiguration config) {
      Properties p = new Properties();
      p.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, RAMJobStore.class.getName());
      return p;
   }

}
