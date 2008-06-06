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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.management.ManagementFactory;

/**
 * The ehcache server.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class ServerContext implements ServletContextListener {


    /**
     * The CacheManager singleton used by this server
     */
    public static CacheManager getCacheManager() {
        return CacheManager.getInstance();
    }


    /**
     * Notification that the web application initialization process is starting. All ServletContextListeners
     * are notified of context initialization before any filter or servlet in the web application is initialized.
     */
    public void contextInitialized(ServletContextEvent sce) {
        ManagementService.registerMBeans(getCacheManager(), ManagementFactory.getPlatformMBeanServer(),
                true, true, true, true);
    }

    /**
     * Notification that the servlet context is about to be shut down. All servlets and filters have been
     * destroy()ed before any ServletContextListeners are notified of context destruction.
     */
    public void contextDestroyed(ServletContextEvent sce) {
        getCacheManager().shutdown();
    }
}