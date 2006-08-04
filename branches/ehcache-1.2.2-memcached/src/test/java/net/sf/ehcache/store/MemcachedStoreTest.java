package net.sf.ehcache.store;

import com.danga.MemCached.SockIOPool;

/**
 * @author Greg Luck
 * @version $Id$
 *
 * Tests for memcached. Note, you will need a functioning memcached to run against. You may need to modify the
 * build.properties.
 */
public class MemcachedStoreTest {

   public void testMemcached() {
       String[] serverlist = { "cache1.int.meetup.com:12345", "cache0.int.meetup.com:12345" };

        // initialize the pool for memcache servers
        SockIOPool pool = SockIOPool.getInstance();
        pool.setServers(serverlist);

        pool.setInitConn(5);
        pool.setMinConn(5);
        pool.setMaxConn(50);
        pool.setMaintSleep(30);
   }


}
