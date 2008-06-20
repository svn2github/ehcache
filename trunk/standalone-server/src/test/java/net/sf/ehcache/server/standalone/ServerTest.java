package net.sf.ehcache.server.standalone;

import org.junit.Test;

import java.io.IOException;

/**
 * Tests the server on its own. 
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class ServerTest {

    /**
     * Use for manual testing
     * GF embedded is broken
     */
    @Test
    public void manualTest() throws IOException {
        Server.main(new String[]{"8080", "/Users/gluck/work/ehcache/server/target/ehcache-server-1.6.0-beta1.war"});
        System.in.read();
    }

}
