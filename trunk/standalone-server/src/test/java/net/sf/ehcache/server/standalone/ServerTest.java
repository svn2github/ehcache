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
     * @throws java.io.IOException
     * @throws InterruptedException
     * todo make work with war dep
     */
    @Test
    public void testManual() throws IOException, InterruptedException {
        Server.main(new String[]{"8080", "/Users/gluck/work/ehcache/server/target/ehcache-server-1.6.0-beta1.war"});
        //System.in.read();
        Thread.sleep(5000);
    }

}
