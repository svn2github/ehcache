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

package net.sf.ehcache.server;


import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.management.ManagementFactory;
import java.util.List;


/**
 * Listens to servlet context events.
 * <p/>
 * This class will register Ehcache's JMX insturmentation with the platform MBean Server. Follow the instructions in
 * your app server to expose the app server's platform MBean server so that you can connect to and monitor it.
 * <p/>
 * When the web app shutsdown, an orderly shutdown of ehcache will commence. Any peristent disk stores will be preserved.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 */
public class ServerContext implements ServletContextListener {


    private static final Logger LOG = LoggerFactory.getLogger(ServerContext.class);
    
    private ManagementService managementService;


    /**
     * Notification that the web application initialization process is starting. All ServletContextListeners
     * are notified of context initialization before any filter or servlet in the web application is initialized.
     */
    public void contextInitialized(ServletContextEvent sce) {
        System.setProperty("com.sun.management.jmxremote", "");
        LOG.info("Starting JMS MBeanServer");
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        managementService = new ManagementService(CacheManager.getInstance(), mBeanServer,
                true, true, true, true, true);
    }


    /**
     * Notification that the servlet context is about to be shut down. All servlets and filters have been
     * destroyed before any ServletContextListeners are notified of context destruction.
     * <p/>
     * Shuts down all cache managers known to {@link CacheManager#ALL_CACHE_MANAGERS}
     */
    public void contextDestroyed(ServletContextEvent sce) {
        //shutdown management service
        try {
            managementService.dispose();
        } catch (CacheException e) {
            LOG.error("", e.getMessage());
        }
        LOG.info("Shutting down Ehcache.");

        //shutdown cache managers
        List knownCacheManagers = CacheManager.ALL_CACHE_MANAGERS;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Shutting down " + knownCacheManagers.size() + " CacheManagers.");
        }
        while (!knownCacheManagers.isEmpty()) {
            CacheManager.ALL_CACHE_MANAGERS.get(0).shutdown();
        }
    }
}