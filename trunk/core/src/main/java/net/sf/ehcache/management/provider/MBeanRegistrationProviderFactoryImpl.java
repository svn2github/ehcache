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
package net.sf.ehcache.management.provider;

import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.Configuration.Monitoring;
import net.sf.ehcache.management.sampled.SampledMBeanRegistrationProvider;

/**
 * Defult implementation of {@link MBeanRegistrationProvider}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class MBeanRegistrationProviderFactoryImpl implements MBeanRegistrationProviderFactory {

    private static final MBeanRegistrationProvider DEFAULT_PROVIDER = new NullMBeanRegistrationProvider();

    /**
     * {@inheritDoc}
     */
    public MBeanRegistrationProvider createMBeanRegistrationProvider(final Configuration config) {
        if (null == config) {
            throw new IllegalArgumentException("Configuration cannot be null.");
        }
        MBeanRegistrationProvider provider;
        if (shouldRegisterMBeans(config)) {
            provider = new SampledMBeanRegistrationProvider();
        } else {
            provider = DEFAULT_PROVIDER;
        }
        return provider;
    }

    private boolean shouldRegisterMBeans(final Configuration config) {
        Monitoring monitoring = config.getMonitoring();
        switch (monitoring) {
        case AUTODETECT:
            return isTcActive();
        case ON:
            return true;
        case OFF:
            return false;
        default:
            throw new IllegalArgumentException("Unknown type of monitoring specified in config: " + monitoring);
        }
    }

    private boolean isTcActive() {
        // do not use a static final to store this in a class.
        // If unclustered cacheManager's are created before creating
        // clustered ones, mbeans will never get registered
        return Boolean.getBoolean("tc.active");
    }
}
