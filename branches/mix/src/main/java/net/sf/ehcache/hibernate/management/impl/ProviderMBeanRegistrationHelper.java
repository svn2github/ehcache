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

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.CacheManager;

import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Environment;
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
    private static final int SLEEP_MILLIS = 500;

    private volatile EhcacheHibernateMBeanRegistrationImpl ehcacheHibernateMBeanRegistration;

    /**
     * Registers mbean for the input cache manager and the session factory name
     *
     * @param manager
     *            the backing cachemanager
     * @param properties
     *            session factory config properties
     */
    public void registerMBean(final CacheManager manager, final Properties properties) {
        if (Boolean.getBoolean("tc.active")) {
            ehcacheHibernateMBeanRegistration = new EhcacheHibernateMBeanRegistrationImpl();
            manager.getTimer().scheduleAtFixedRate(new RegisterMBeansTask(ehcacheHibernateMBeanRegistration, manager, properties), SLEEP_MILLIS,
                    SLEEP_MILLIS);
        }
    }

    /**
     * Unregisters previously registered mbean.
     */
    public void unregisterMBean() {
        if (ehcacheHibernateMBeanRegistration != null) {
            ehcacheHibernateMBeanRegistration.dispose();
            ehcacheHibernateMBeanRegistration = null;
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
        private final EhcacheHibernateMBeanRegistrationImpl ehcacheHibernateMBeanRegistration;
        private final CacheManager manager;
        private final Properties properties;

        public RegisterMBeansTask(EhcacheHibernateMBeanRegistrationImpl ehcacheHibernateMBeanRegistration,
                CacheManager manager, Properties properties) {
            this.ehcacheHibernateMBeanRegistration = ehcacheHibernateMBeanRegistration;
            this.manager = manager;
            this.properties = properties;
        }

        @Override
        public void run() {
            LOG.debug("Running mbean initializer task for ehcache hibernate...");
            startTime = System.currentTimeMillis();
            if (mbeanRegistered.compareAndSet(false, true)) {
                try {
                    ehcacheHibernateMBeanRegistration.registerMBeanForCacheManager(manager, properties);
                    LOG.debug("Successfully registered bean");
                } catch (Exception e) {
                    throw new CacheException(e);
                }
            }
            SessionFactory sessionFactory = locateSessionFactory();
            if (sessionFactory == null) {
                LOG.debug("SessionFactory is probably still being initialized..."
                        + " waiting for it to complete before enabling hibernate statistics monitoring via JMX");
                if (System.currentTimeMillis() > startTime + (NUM_SECONDS * MILLIS_PER_SECOND)) {
                    LOG.info("Hibernate statistics monitoring through JMX is DISABLED.");
                    LOG.info("Failed to look up SessionFactory after " + NUM_SECONDS + " seconds using session-factory properties '"
                            + properties + "'");
                    this.cancel();
                }
                return;
            } else {
                ehcacheHibernateMBeanRegistration.enableHibernateStatisticsSupport(sessionFactory);
                LOG.info("Hibernate statistics monitoring through JMX is ENABLED. ");
                this.cancel();
            }
        }

        private SessionFactory locateSessionFactory() {
            String jndiName = properties.getProperty(Environment.SESSION_FACTORY_NAME);
            if (jndiName != null) {
                return (SessionFactory)SessionFactoryObjectFactory.getNamedInstance(jndiName);
            }
            try {
                Class factoryType = SessionFactoryObjectFactory.class;
                Field instancesField = getField(factoryType, "INSTANCES");
                if (instancesField == null) {
                    throw new RuntimeException("Expected INSTANCES field on " + SessionFactoryObjectFactory.class.getName());
                }
                instancesField.setAccessible(true);
                Map map = (Map)instancesField.get(null);
                if (map == null) {
                    return null;
                }
                Iterator values = map.values().iterator();
                while (values.hasNext()) {
                    SessionFactory sessionFactory = (SessionFactory)values.next();
                    Class sessionFactoryType = sessionFactory.getClass();
                    Field propertiesField = getField(sessionFactoryType, "properties");
                    if (propertiesField != null) {
                        propertiesField.setAccessible(true);
                        Properties props = (Properties)propertiesField.get(sessionFactory);
                        if (props != null && props.equals(properties)) {
                            return sessionFactory;
                        }
                    }
                }
            } catch (RuntimeException re) {
                LOG.error("Error locating Hibernate Session Factory", re);
            } catch (IllegalAccessException iae) {
                LOG.error("Error locating Hibernate Session Factory", iae);
            }
            return null;
        }
    }

    private static Field getField(Class c, String fieldName) {
        for (Field field : c.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        throw new NoSuchFieldError("Type '" + c + "' has no field '" + fieldName + "'");
    }
}
