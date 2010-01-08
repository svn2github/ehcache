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

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.CacheManager;

import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.impl.SessionFactoryObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for registering mbeans for ehcache backed hibernate second level cache
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public class ProviderMBeanRegistrationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ProviderMBeanRegistrationHelper.class);

    private static final int MILLIS_PER_SECOND = 1000;
    private static final int SLEEP_MILLIS = 200;

    /**
     * Registers mbean for the input cache manager and the session factory name
     * 
     * @param manager
     *            the backing cachemanager
     * @param sessionFactoryNameFromConfig
     *            session factory name from the hibernate config
     */
    public void registerMBean(final CacheManager manager, final String sessionFactoryNameFromConfig) {
        if (Boolean.getBoolean("tc.active")) {
            manager.getTimer().scheduleAtFixedRate(new RegisterMBeansTask(manager, sessionFactoryNameFromConfig), SLEEP_MILLIS,
                    SLEEP_MILLIS);
        }
    }

    /**
     * 
     * Task for running mbean registration that can be scheduled in a timer
     * 
     */
    private static class RegisterMBeansTask extends TimerTask {

        private static final int NUM_SECONDS = 30;
        private long startTime;
        private final AtomicBoolean mbeanRegistered = new AtomicBoolean(false);
        private EhcacheHibernateMBeanRegistration ehcacheHibernateMBeanRegistration = new EhcacheHibernateMBeanRegistrationImpl();
        private final CacheManager manager;
        private final String sessionFactoryNameFromConfig;

        public RegisterMBeansTask(CacheManager manager, String sessionFactoryNameFromConfig) {
            this.manager = manager;
            this.sessionFactoryNameFromConfig = sessionFactoryNameFromConfig;
        }

        @Override
        public void run() {
            LOG.debug("Running mbean initializer task for ehcache hibernate...");
            if (mbeanRegistered.compareAndSet(false, true)) {
                startTime = System.currentTimeMillis();
                try {
                    ehcacheHibernateMBeanRegistration.registerMBeanForCacheManager(manager, sessionFactoryNameFromConfig);
                    LOG.debug("Successfully registered bean");
                } catch (Exception e) {
                    throw new CacheException(e);
                }
            }
            if (null == sessionFactoryNameFromConfig || "".equals(sessionFactoryNameFromConfig.trim())) {
                LOG.info("Hibernate statistics monitoring through JMX is DISABLED. "
                        + "For enabling, use the property 'hibernate.session_factory_name' "
                        + "to provide a name for the session-factory in the hibernate configuration.");
                this.cancel();
                return;
            }
            SessionFactory sessionFactory = (SessionFactory) SessionFactoryObjectFactory.getNamedInstance(sessionFactoryNameFromConfig);
            if (sessionFactory == null) {
                LOG.debug("Session factory is probably still getting initialized..."
                        + " waiting for it to complete before enabling hibernate statistics monitoring via JMX");
                if (System.currentTimeMillis() > startTime + (NUM_SECONDS * MILLIS_PER_SECOND)) {
                    LOG.info("Hibernate statistics monitoring through JMX is DISABLED.");
                    LOG.info("Failed to look up Session Factory even after " + NUM_SECONDS + " seconds using session-factory name '"
                            + sessionFactoryNameFromConfig + "'");
                    this.cancel();
                }
                return;
            } else {
                ehcacheHibernateMBeanRegistration.enableHibernateStatisticsSupport(sessionFactory);
                LOG.info("Hibernate statistics monitoring through JMX is ENABLED. ");
                this.cancel();
            }
        }
    }

}
