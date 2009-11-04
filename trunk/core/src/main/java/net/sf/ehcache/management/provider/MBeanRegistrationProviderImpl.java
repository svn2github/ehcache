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

import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.Configuration.Monitoring;
import net.sf.ehcache.management.sampled.SampledMBeanRegistrationProvider;

/**
 * Implementation of {@link MBeanRegistrationProvider}
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7.1
 */
public class MBeanRegistrationProviderImpl implements MBeanRegistrationProvider {

    private final SampledMBeanRegistrationProvider sampledProvider;

    private final Monitoring monitoring;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private CacheManager cachedCacheManager;

    /**
     * Constructor accepting the {@link Configuration}
     * 
     * @param configuration
     */
    public MBeanRegistrationProviderImpl(Configuration configuration) {
        sampledProvider = new SampledMBeanRegistrationProvider();
        this.monitoring = configuration.getMonitoring();
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(CacheManager cacheManager) throws MBeanRegistrationProviderException {
        if (!initialized.getAndSet(true)) {
            if (shouldRegisterMBeans()) {
                sampledProvider.initialize(cacheManager);
            }
            this.cachedCacheManager = cacheManager;
        } else {
            throw new IllegalStateException("MBeanRegistrationProvider is already initialized");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void reinitialize() throws MBeanRegistrationProviderException {
        if (shouldRegisterMBeans()) {
            if (sampledProvider.isAlive()) {
                sampledProvider.reinitialize();
            } else {
                sampledProvider.initialize(cachedCacheManager);
            }
        }
    }

    private boolean shouldRegisterMBeans() {
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
        // clustered ones, mbeans will never get registered (in standalone context)
        return Boolean.getBoolean("tc.active");
    }

}
