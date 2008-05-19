package net.sf.ehcache.server;

import com.sun.ws.rest.api.container.httpserver.HttpServerFactory;

import java.io.IOException;
import java.io.File;

/**
 * The ehcache server
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class Server {

    public static void main(String[] args) throws IOException {
        GlassFish glassfish = new GlassFish();
// create smallest possible HTTP set up listening on port 8080
glassfish.minimallyConfigure(8080);

GFApplication app = glassfish.deploy(new File("path/to/simple.war"));
             
             System.out.println("Server running");
             System.out.println("Visit: http://localhost:9998/helloworld");
             System.out.println("Hit return to stop...");
             System.in.read();
             System.out.println("Stopping server");
             app.undeploy();
             glassfish.stop();
             System.out.println("Server stopped");
         }



}
