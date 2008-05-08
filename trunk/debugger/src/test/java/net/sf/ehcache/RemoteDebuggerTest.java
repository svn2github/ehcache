package net.sf.ehcache;

import junit.framework.TestCase;
import net.sf.ehcache.distribution.RemoteDebugger;

/**
 * A place holder to stop maven from falling over with an NPE
 */
public class RemoteDebuggerTest extends TestCase {


    /**
     * Make sure main with no params does not explode
     */
    public void testMainEmpty() throws InterruptedException {

        RemoteDebugger.main(new String[] {});
    }

}
