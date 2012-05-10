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

package net.sf.ehcache.constructs.web;

import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;




import java.util.List;

/**
 * A ServletContextListener that shutsdown CacheManager. Use this when you want to shutdown
 * ehcache automatically when the web application is shutdown.
 * <p/>
 * To receive notification events, this class must be configured in the deployment
 * descriptor for the web application.
 *
 * To do so, add the following to web.xml in your web application:
 * <pre>
 * &lt;listener&gt;
 *      &lt;listener-class&gt;net.sf.ehcache.constructs.web.ShutdownListener&lt;/listener-class&gt;
 * &lt;/listener&gt;
 * <p/>
 * </pre>
 *
 * @author Daniel Wiell
 * @author Greg Luck
 * @version $Id: ShutdownListener.java 744 2008-08-16 20:10:49Z gregluck $
 */
public class ShutdownListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownListener.class);

    /**
     * Notification that the web application is ready to process requests.
     *
     * @param servletContextEvent
     */
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        //nothing required
    }

    /**
     * Notification that the servlet context is about to be shut down.
     * <p/>
     * Shuts down all cache managers known to {@link CacheManager#ALL_CACHE_MANAGERS}
     *
     * @param servletContextEvent
     */
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        List knownCacheManagers = CacheManager.ALL_CACHE_MANAGERS;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Shutting down " + knownCacheManagers.size() + " CacheManagers.");
        }
        while (!knownCacheManagers.isEmpty()) {
            ((CacheManager) CacheManager.ALL_CACHE_MANAGERS.get(0)).shutdown();
        }
    }
}
