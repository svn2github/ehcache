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
import org.glassfish.embed.GFException;
import org.glassfish.embed.GlassFish;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
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
        //
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
     * The CacheManager singleton used by this server
     */
    public static CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Starts the server and registers ehcache with the JMX Platform MBeanServer
     */
    public void init() {
        if (ehcacheServerWar == null) {
            serverThread = new HttpServerThread();
        } else {
            serverThread = new GlassfishServerThread();
        }
        serverThread.start();

        ManagementService.registerMBeans(Server.getCacheManager(), ManagementFactory.getPlatformMBeanServer(),
                true, true, true, true);
    }

    /**
     * Shutsdown the HTTP server.
     */
    public void destroy() {
        serverThread.stopServer();
    }

    /**
     * Usage: java -classpath ... net.sf.ehcache.server.Server &lt;port&gt; path to ehcache-server.war
     * <p/>
     * The port is optional. It should be <= 65536
     * <p/>
     * If no war is specified, the LightWeight Http server is used. Otherwise Glassfish is used.
     *
     * @param args The first argument is the server port. The second is optional and is the path to the ehcache-server.war.
     */
    public static void main(String[] args) throws IOException {
        Server server = null;
        if (args.length == 1 && args[0].matches("--help")) {
            System.out.println("java -classpath ... net.sf.ehcache.server.Server <http port> <ehcache-server.war>} ");
            System.exit(0);
        }
        if (args.length == 1) {
            Integer port = Integer.parseInt(args[0]);
            server = new Server(port, null);
            server.init();
        }
        if (args.length == 2) {
            Integer port = Integer.parseInt(args[0]);
            File war = new File(args[1]);
            server = new Server(port, war);
            server.init();
        }
    }

    /**
     * Forks the Server into its own thread.
     */
    abstract class ServerThread extends Thread {

        /**
         * Stops the server.
         */
        public abstract void stopServer();
    }

    /**
     * Embedded Glassfish implementation
     */
    class GlassfishServerThread extends ServerThread {

        private GlassFish glassfish;


        /**
         * Creates a server in a separate thread.
         * <p/>
         * This permits the calling thread to immediately return
         */
        public void run() {
            startWithGlassfish();
        }


        /**
         * Glassfish embedding API is currently broken
         *
         * @throws IOException
         */
        private void startWithGlassfish() {

            try {
                glassfish = new GlassFish(listeningPort);
                //broken in latest snapshot
                //GFApplication application = glassfish.deploy(ehcacheServerWar);
                LOG.info("Glassfish server running on port " + listeningPort + " with WAR " + ehcacheServerWar);
            } catch (GFException e) {
                LOG.log(Level.SEVERE, "Cannot start server. ", e);
            }
        }

        /**
         * Stops the server
         */
        public void stopServer() {
            glassfish.stop();
        }
    }

    /**
     * A server thread for the Lightweight HttpServer built into Java 6.
     */
    class HttpServerThread extends ServerThread {

        private HttpServer server;

        /**
         * Starts the server in a separate thread.
         */
        public synchronized void start() {
            startWithLightWeightHttpServer();
        }

        private void startWithLightWeightHttpServer() {
            PackagesResourceConfig prc = new PackagesResourceConfig(new String[]{"net.sf.ehcache.server.rest.resources"});
            HttpHandler h = ContainerFactory.createContainer(HttpHandler.class, prc);
            try {
                server = HttpServerFactory.create("http://localhost:" + listeningPort + "/ehcache", h);
                server.start();
                LOG.info("Lightweight HTTP Server running on port " + listeningPort);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Cannot start server. ", e);
            }
        }

        /**
         * Stops the server.
         */
        public void stopServer() {
            server.stop(0);
        }
    }

}
