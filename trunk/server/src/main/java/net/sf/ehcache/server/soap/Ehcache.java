/**
 * Created by IntelliJ IDEA.
 * User: gluck
 * Date: May 20, 2008
 * Time: 9:23:55 AM
 * To change this template use File | Settings | File Templates.
 */
package net.sf.ehcache.server.soap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * The Ehcache WebService
 */
@WebService()
public class Ehcache {

    private static final Log LOG = LogFactory.getLog(Ehcache.class.getName());

    /**
     * Test method
     * @param from
     * @return
     */
    @WebMethod
    public String sayHelloWorldFrom(String from) {
        String result = "Hello, world, from " + from;
        System.out.println(result);
        return result;
    }
}