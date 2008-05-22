package net.sf.ehcache.server.resources;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.ProduceMime;

/**
 * Ehcache Web Resource
 * <p/>
 * This resource will be hosted at the URI path "/ehcache"
 *
 * @author Greg Luck
 * @version $Id$
 */
@Path("/ehcache")
public class EhcacheResource {

    /**
     * Creates a new instance of HelloWorldResource
     */
    public EhcacheResource(String greg) {
    }

    /**
     * Retrieves representation of an instance of hello.world.HelloWorldResource
     *
     * @return an instance of java.lang.String
     */
    @GET
    @ProduceMime("text/plain")
    public String getClichedMessage() {
        return "Hello ehcache!";
    }


}
