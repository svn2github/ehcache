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
package net.sf.ehcache.management.provider;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;

/**
 * Implementations of this interface will can initialize MBeanRegistration for
 * the passed CacheManager.
 * This is in addition to the ManagementService and has nothing to do
 *
 * <p />
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface MBeanRegistrationProvider {

    /**
     * Initialize MBeanRegistration if necessary for the cacheManager
     *
     * @param cacheManager
     * @throws MBeanRegistrationProviderException
     */
    void initialize(CacheManager cacheManager, ClusteredInstanceFactory clusteredInstanceFactory)
      throws MBeanRegistrationProviderException;

    /**
     * Reinitialize the mbeans. Uses the current name of the {@link CacheManager} to re-register the mbeans
     *
     * @throws MBeanRegistrationProviderException
     */
    void reinitialize(ClusteredInstanceFactory clusteredInstanceFactory) throws MBeanRegistrationProviderException;

    /**
     * Returns {@code true} if initialized otherwise false
     * @return {@code true} if initialized
     */
    boolean isInitialized();

}
