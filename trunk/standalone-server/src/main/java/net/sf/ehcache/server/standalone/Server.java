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

package net.sf.ehcache.server.standalone;


import org.glassfish.embed.GFApplication;
import org.glassfish.embed.GFException;
import org.glassfish.embed.GlassFish;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ehcache server.
 * <p/>
 * This version uses the Java 6 built-in lightweight HTTP server, which is not suitable for production,
 * according to the research I have done.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class Server {


    /**
     * Default port: 8080
     */
    public static final Integer DEFAULT_PORT = 8080;

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
     * Starts the server and registers ehcache with the JMX Platform MBeanServer
     */
    public void init() {
        serverThread = new GlassfishServerThread();
        serverThread.start();

//        ManagementService.registerMBeans(Server.getCacheManager(), ManagementFactory.getPlatformMBeanServer(),
//                true, true, true, true);
    }

    /**
     * Shuts down the HTTP server.
     */
    public void destroy() {
        serverThread.stopServer();
    }

    /**
     * Usage: java -jar ...  [http port] warfile | wardir
     * <p/>
     * The port is optional. It should be <= 65536
     * <p/>
     * If no war is specified, the LightWeight Http server is used. Otherwise Glassfish is used.
     *
     * @param args The first argument is the server port. The second is optional and is the path to the ehcache-server.war.
     */
    public static void main(String[] args) throws IOException {
        Server server = null;
        if (args.length < 1 || args.length > 2 || (args.length == 1 && args[0].matches("--help"))) {
            System.out.println("Usage: java -jar ...  [http port] warfile | wardir ");
            System.exit(0);
        }
        if (args.length == 1) {
            File war = new File(args[1]);
            System.out.println("Starting standalone ehcache server on port " + DEFAULT_PORT + " with warfile " + war);
            server = new Server(null, war);
            server.init();
        }
        if (args.length == 2) {
            Integer port = Integer.parseInt(args[0]);
            File war = new File(args[1]);
            if (!war.exists()) {
                System.err.println("Error: War file " + war + " does not exist.");
                System.exit(1);
            }
            System.out.println("Starting standalone ehcache server on port " + port + " with warfile " + war);
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
         * @throws IOException
         */
        private void startWithGlassfish() {

            try {
                glassfish = new GlassFish(listeningPort);

                GFApplication application = glassfish.deploy(ehcacheServerWar);
                LOG.info("Glassfish server running on port " + listeningPort + " with WAR " + ehcacheServerWar);
            } catch (GFException e) {
                LOG.log(Level.SEVERE, "Cannot start server. ", e);
            } catch (IOException e) {
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


}
