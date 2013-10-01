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

/**
 * This interface is for a class to generate a custom properties config for a 
 * Quartz Job Store initialization.
 * @author cschanck
 *
 */
public interface ScheduledRefreshJobStorePropertiesFactory {
   
   /**
    * Job store properties. These properties will be used to initialize Quartz. At 
    * a minimum, must provide the job store class, plus any an all additional job store
    * configuration properties needed (for JDBC, for example, there are *lots* of other
    * config parameters). Note that excess propreties passed by the configuration of the cache are
    * available from the configuration.
    * @param config 
    * @param underlyingCache 
    *
    * @return the properties
    */
   Properties jobStoreProperties(Ehcache underlyingCache, ScheduledRefreshConfiguration config);
}
