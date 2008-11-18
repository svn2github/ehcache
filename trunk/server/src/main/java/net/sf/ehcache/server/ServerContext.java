/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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


import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

/**
 * The ehcache server.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class ServerContext implements ServletContextListener {


    private static final Logger LOG = Logger.getLogger(ServerContext.class.getName());

    private JMXConnectorServer jmxConnectorServer;


    /**
     * Notification that the web application initialization process is starting. All ServletContextListeners
     * are notified of context initialization before any filter or servlet in the web application is initialized.
     */
    public void contextInitialized(ServletContextEvent sce) {
        System.setProperty("com.sun.management.jmxremote", "");
        LOG.info("Starting JMS MBeanServer");
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ManagementService.registerMBeans(CacheManager.getInstance(), mBeanServer,
                true, true, true, true);


//        try {
//            final String hostname = InetAddress.getLocalHost().getHostName();
//            int port = 8081;
//            LocateRegistry.createRegistry(port);
//            JMXServiceURL url = new JMXServiceURL(
//                    "service:jmx:rmi://" + hostname + ":" + port + "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi");
//            jmxConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mBeanServer);
//            LOG.info("Start the RMI connector server on url " + url);
//            jmxConnectorServer.start();
//
//        } catch (Exception e) {
//            LOG.severe(e.getMessage());
//
//        }

    }

    /**
     * Notification that the servlet context is about to be shut down. All servlets and filters have been
     * destroyed before any ServletContextListeners are notified of context destruction.
     */
    public void contextDestroyed(ServletContextEvent sce) {
//        try {
//            jmxConnectorServer.stop();
//        } catch (IOException e) {
//            LOG.severe(e.getMessage());
//        }
    }
}