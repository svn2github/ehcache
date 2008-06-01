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
import org.glassfish.embed.GFApplication;
import org.glassfish.embed.GlassFish;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

/**
 * The ehcache server.
 * <p/>
 * This version uses the Java 6 built-in lightweight HTTP server, which is not suitable for production,
 * according to the research I have done.
 * todo replace with GFV3 or something hardcore. Trying...hard...
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class Server {


    /**
     * Default port: 8080
     */
    public static final Integer DEFAULT_PORT = 9998;

    /**
     * The singleton CacheManager instance exposed by this server.
     */
    private static CacheManager cacheManager = CacheManager.getInstance();

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private Integer listeningPort = DEFAULT_PORT;

    private File ehcacheServerWar;

    private ServerThread serverThread;


    /**
     * Empty constructor.
     * This will create a server listening on the default port of 9998 and using the LightWeight HTTP Server
     *
     * @see #Server(Integer, java.io.File)
     */
    public Server() {

    }

    /**
     * Constructs a server on a given port
     *
     * @param listeningPort the port to listen on.
     */
    public Server(Integer listeningPort, File ehcacheServerWar) {
        this.listeningPort = listeningPort;
        this.ehcacheServerWar = ehcacheServerWar;
    }

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
     * todo make this the real one using Glassfish V3 Embedded
     * Usage: java -classpath ... net.sf.ehcache.server.Server {path to ehcache-server.war}
     */
    public static void main(String[] args) throws IOException {
        Server server = null;
        if (args.length == 1 && args[0].matches("--help")) {
            System.out.println("java -classpath ... net.sf.ehcache.server.Server {path to ehcache-server.war} {http port}");
            System.exit(0);
        }
        if (args.length == 2) {
            Integer port = Integer.parseInt(args[0]);
            File war = new File(args[1]);
            server = new Server(port, war);
        }
        server.init();
    }

    /**
     * Forks the Server into its own thread.
     */
    class ServerThread extends Thread {

        /**
         * Creates a server in a separate thread.
         * <p/>
         * This permits the calling thread to immediately return
         */
        public void run() {
            try {
                if (ehcacheServerWar == null) {
                    startWithLightWeightHttpServer();
                } else {
                    startWithGlassfish();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        private void startWithLightWeightHttpServer() throws IOException {
            PackagesResourceConfig prc = new PackagesResourceConfig(new String[]{"net.sf.ehcache.server.resources"});
            HttpHandler h = ContainerFactory.createContainer(HttpHandler.class, prc);
            HttpServer server = HttpServerFactory.create("http://localhost:" + listeningPort + "/ehcache", h);
            server.start();
            LOG.info("Lightweight HTTP Server running on port " + listeningPort);
        }

        private void startWithGlassfish() throws IOException {
            GlassFish glassfish = new GlassFish(listeningPort);
            GFApplication application = glassfish.deploy(ehcacheServerWar);
            LOG.info("Glassfish server running on port " + listeningPort + " with WAR " + ehcacheServerWar);
        }

    }

    /**
     * The CacheManager singleton used by this server
     */
    public static CacheManager getCacheManager() {
        return cacheManager;
    }
}
