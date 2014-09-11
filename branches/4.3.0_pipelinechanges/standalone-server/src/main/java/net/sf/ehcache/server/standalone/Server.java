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


import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;

import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The ehcache server.
 * <p/>
 * By default the server listens for HTTP at 8080 and JMX at 8081.
 * <p/>
 * The HTTP Port may be passed in. The JMX port is always the HTTP port + 1. So, if the HTTP port is 9076, the JMX
 * listening port will be 9077.
 * <p/>
 * No other ports are opened.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class Server implements Daemon {


    /**
     * Default port: 8080
     */
    public static final Integer DEFAULT_BASE_PORT = 8080;

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private static ServerThread serverThread;

    private DaemonController controller;
    private File war;
    private Integer httpPort = DEFAULT_BASE_PORT;

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
     * Constructs a server listening at the given HTTP port
     *
     * @param httpPort         the HTTP port to listen on. The JMX connector port is always to set to the HTTP port + 1.
     * @param ehcacheServerWar the ehcache-server.war
     */
    public Server(Integer httpPort, File ehcacheServerWar) {
        this.httpPort = httpPort;
        this.war = ehcacheServerWar;
    }


    /**
     * Invoked by Jsvc with a context which includes the arguments passed to Jsvc. The main() method
     * also calls through here so that the server can be started using either Jsvc or java -jar...
     *
     * @param daemonContext
     * @throws Exception
     */
    public void init(DaemonContext daemonContext) throws Exception {
        String[] args = daemonContext.getArguments();
        if (args.length < 1 || args.length > 2 || (args.length == 1 && args[0].matches("--help"))) {
            System.out.println("Usage: java -jar ...  [http httpPort] warfile | wardir ");
            System.exit(0);
        }
        if (args.length == 1) {
            war = new File(args[0]);
            if (!war.exists()) {
                System.err.println("Error: War file or exploded directory " + war + " does not exist.");
                System.exit(1);
            }
        }
        if (args.length == 2) {
            httpPort = Integer.parseInt(args[0]);
            war = new File(args[1]);
            if (!war.exists()) {
                System.err.println("Error: War file or exploded directory " + war + " does not exist.");
                System.exit(1);
            }
        }

        /* Dump a message */
        System.err.println("\nEhcache standalone server initializing...");

        /* Set up this simple daemon */
        this.controller = daemonContext.getController();
    }

    /**
     * Starts the server. This method is called by Jsvc. The main() method
     * also calls through here so that the server can be started using either Jsvc or java -jar...
     *
     * @throws Exception
     */
    public void start() throws Exception {
        System.out.println("\nStarting standalone ehcache server on httpPort " + httpPort + " with WAR file or directory " + war);
        serverThread = new GlassfishServerThread();
        serverThread.start();
    }

    /**
     * A method for stopping a server. This is meant to be used by integration tests thus the package protected.
     *
     * @throws InterruptedException if the server is interrupted while stopping
     */
    static void stopStatic() throws InterruptedException, GlassFishException {
        serverThread.stopServer();
        //wait indefinitely until it shuts down
        serverThread.join();


    }

    /**
     * Shuts down the HTTP server in an orderly way.
     */
    public void stop() throws InterruptedException, GlassFishException {
        System.out.println("\nEhcache standalone server stopping...");
        stopStatic();
        System.out.println("\nEhcache standalone server stopped.");
    }

    /**
     * Interrupts the server thread.
     */
    public void destroy() {
        System.out.println("\nEhcache standalone server destroyed.");
        serverThread.interrupt();
    }


    /**
     * A mock DaemonContext which allows the main() method to call <code>init</code>.
     */
    static class MockDaemonContext implements DaemonContext {
        private String[] args;

        /**
         * Constructor.
         *
         * @param args the <code>main()</code> method arguments.
         */
        public MockDaemonContext(String[] args) {
            this.args = args;
        }

        /**
         * @return null as it is not a real Daemon
         */
        public DaemonController getController() {
            return null;
        }

        /**
         * @return the arguments <code>main()</code> was invoked with.
         */
        public String[] getArguments() {
            return args;
        }
    }

    /**
     * Wires in main() to use the Jsvc invocation path.
     * <p/>
     * Usage: java -jar ...  [http port] warfile | wardir
     * <p/>
     * The port is optional. It should be <= 65536
     * <p/>
     * If no war is specified, the LightWeight Http server is used. Otherwise Glassfish is used.
     *
     * @param args The first argument is the server port. The second is optional and is the path to the ehcache-server.war.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

//        String[] arguments = {"9090", "target/war/work/net.sf.ehcache/ehcache-server/greg.war"};

        Server server = new Server();
        server.init(new MockDaemonContext(args));
        server.start();
    }

    /**
     * Forks the Server into its own thread.
     */
    abstract class ServerThread extends Thread {

        /**
         * Stops the server.
         */
        public abstract void stopServer() throws GlassFishException;
    }

    /**
     * Embedded Glassfish implementation
     */
    class GlassfishServerThread extends ServerThread {

        private GlassFish server;

        /**
         * Creates a server in a separate thread.
         * <p/>
         * This permits the calling thread to immediately return
         */
        public void run() {
            startWithGlassfish();
        }


        private void startWithGlassfish() {

            try {
                GlassFishRuntime gfr = GlassFishRuntime.bootstrap();
                GlassFishProperties glassfishProps = new GlassFishProperties();
                glassfishProps.setPort("http-listener", httpPort);
                Integer jmxPort = httpPort + 1;
                // todo embeddedInfo.setJmxConnectorPort(jmxPort);
                // Jerome Dochez confirmed 4 March 2010 this did not make it into 3 embedded

                server = gfr.newGlassFish(glassfishProps);
                server.start();
                Deployer deployer = server.getDeployer();
                deployer.deploy(war, "--contextroot=ehcache", "--enabled=true", "--force=true");

                LOG.info("Glassfish server running on httpPort " + httpPort + " with WAR " + war);
                //+ ". JMX is listening at " + jmxPort);


            } catch (Exception e) {
                LOG.error("Cannot start server. ", e);
            }
        }

        /**
         * Stops the server
         */
        public void stopServer() throws GlassFishException {
            //will cause the startsWithGlassfish method to return, and thus run() thus ending the thread.
            server.stop();
        }
    }


}
