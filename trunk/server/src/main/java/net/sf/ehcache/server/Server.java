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


import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

/**
 * The ehcache server.
 * <p/>
 * This version uses the Java 6 built-in lightweight HTTP server, which is not suitable for production,
 * according to the research I have done.
 * todo replace with GFV3 or something hardcore
 *
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class Server {


    /**
     * The singleton CacheManager instance exposed by this server.
     */
    private static CacheManager cacheManager = CacheManager.getInstance();

    private static final Logger LOG = Logger.getLogger(Server.class.getName());
    private ServerThread serverThread;

    /**
     * Starts the server and registers ehcache with the JMX Platform MBeanServer
     */
    public void init() {
        serverThread = new ServerThread();
        serverThread.start();

        ManagementService.registerMBeans(Server.getCacheManager(), ManagementFactory.getPlatformMBeanServer(),
                true, true, true, true);
    }

    /**
     *
     */
    public void destroy() {
        serverThread.interrupt();        
    }

    /**
     * Use for manual testing
     * todo make this the real one using Glassfish V3 Embedded
     */
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.init();

    }

//        GlassFish glassfish = new GlassFish();
//// create smallest possible HTTP set up listening on port 8080
//glassfish.minimallyConfigure(8080);
//
//GFApplication app = glassfish.deploy(new File("path/to/simple.war"));
//
//             System.in.read();
//             app.undeploy();
//             glassfish.stop();


    /**
     * Forks the Server into its own thread.
     */
    static class ServerThread extends Thread {

        /**
         * If this thread was constructed using a separate
         * <code>Runnable</code> run object, then that
         * <code>Runnable</code> object's <code>run</code> method is called;
         * otherwise, this method does nothing and returns.
         * <p/>
         * Subclasses of <code>Thread</code> should override this method.
         *
         * @see Thread#start()
         * @see Thread#stop()
         * @see Thread#Thread(ThreadGroup,
         *      Runnable, String)
         * @see Runnable#run()
         */
        public void run() {
            try {

                PackagesResourceConfig prc = new PackagesResourceConfig(new String[]{"net.sf.ehcache.server.resources"});
                HttpHandler h = ContainerFactory.createContainer(HttpHandler.class, prc);
                HttpServer server = HttpServerFactory.create("http://localhost:9998/ehcache", h);
                server.start();

                LOG.info("Server running");
                LOG.info("Visit: http://localhost:9998/ehcache");
                LOG.info("Hit return to stop...");
//                System.in.read();
//                LOG.info("Stopping server");
//                server.stop(0);
//                LOG.info("Server stopped");
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }

    /**
     * The CacheManager singleton used by this server
     */
    public static CacheManager getCacheManager() {
        return cacheManager;
    }
}
