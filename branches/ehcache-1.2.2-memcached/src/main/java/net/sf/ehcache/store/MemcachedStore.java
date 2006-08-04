package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Status;
import com.danga.MemCached.SockIOPool;

/**
 * /**
 * This class is a connection pool for maintaning a pool of persistent connections<br/>
 * to memcached servers.
 * <p/>
 * The pool must be initialized prior to use. This should typically be early on<br/>
 * in the lifecycle of the JVM instance.<br/>
 * <br/>
 * <h3>An example of initializing using defaults:</h3>
 * <pre>
 * <p/>
 * 	static {
 * 		String[] serverlist = { "cache0.server.com:12345", "cache1.server.com:12345" };
 * <p/>
 * 		SockIOPool pool = SockIOPool.getInstance();
 * 		pool.setServers(serverlist);
 * 		pool.initialize();
 * 	}
 * </pre>
 * <h3>An example of initializing using defaults and providing weights for servers:</h3>
 * <pre>
 * 	static {
 * 		String[] serverlist = { "cache0.server.com:12345", "cache1.server.com:12345" };
 * 		Integer[] weights   = { new Integer(5), new Integer(2) };
 * <p/>
 * 		SockIOPool pool = SockIOPool.getInstance();
 * 		pool.setServers(serverlist);
 * 		pool.setWeights(weights);
 * 		pool.initialize();
 * 	}
 *  </pre>
 * <h3>An example of initializing overriding defaults:</h3>
 * <pre>
 * 	static {
 * 		String[] serverlist     = { "cache0.server.com:12345", "cache1.server.com:12345" };
 * 		Integer[] weights       = { new Integer(5), new Integer(2) };
 * 		int initialConnections  = 10;
 * 		int minSpareConnections = 5;
 * 		int maxSpareConnections = 50;
 * 		long maxIdleTime        = 1000 * 60 * 30;	// 30 minutes
 * 		long maxBusyTime        = 1000 * 60 * 5;	// 5 minutes
 * 		long maintThreadSleep   = 1000 * 5;			// 5 seconds
 * 		int	socketTimeOut       = 1000 * 3;			// 3 seconds to block on reads
 * 		int	socketConnectTO     = 1000 * 3;			// 3 seconds to block on initial connections.  If 0, then will use blocking connect (default)
 * 		boolean failover        = false;			// turn off auto-failover in event of server down
 * 		boolean nagleAlg        = false;			// turn off Nagle's algorithm on all sockets in pool
 * <p/>
 * 		SockIOPool pool = SockIOPool.getInstance();
 * 		pool.setServers( serverlist );
 * 		pool.setWeights( weights );
 * 		pool.setInitConn( initialConnections );
 * 		pool.setMinConn( minSpareConnections );
 * 		pool.setMaxConn( maxSpareConnections );
 * 		pool.setMaxIdle( maxIdleTime );
 * 		pool.setMaxBusyTime( maxBusyTime );
 * 		pool.setMaintSleep( maintThreadSleep );
 * 		pool.setSocketTO( socketTimeOut );
 * 		pool.setSocketConnectTO( socketConnectTO );
 * 		pool.setNagle( nagleAlg );
 * 		pool.setHashingAlg( SockIOPool.NEW_COMPAT_HASH );
 * 		pool.initialize();
 * 	}
 *  </pre>
 * The easiest manner in which to initialize the pool is to set the servers and rely on defaults as in the first example.<br/>
 * After pool is initialized, a client will request a SockIO object by calling getSock with the cache key<br/>
 * The client must always close the SockIO object when finished, which will return the connection back to the pool.<br/>
 * <h3>An example of retrieving a SockIO object:</h3>
 * <pre>
 * 		SockIOPool.SockIO sock = SockIOPool.getInstance().getSock( key );
 * 		try {
 * 			sock.write( "version\r\n" );
 * 			sock.flush();
 * 			System.out.println( "Version: " + sock.readLine() );
 * 		}
 * 		catch (IOException ioe) { System.out.println( "io exception thrown" ) };
 * <p/>
 * 		sock.close();
 * </pre>
 *
 * @author Greg Luck
 * @version $Id$
 */
public class MemcachedStore implements Store {

    private static SockIOPool socketIOPool;
    private Status status;

    /** 30 minutes */
    private static final int DEFAULT_IDLE_TIME = 1000 * 60 * 30;

    /**
     * Create a MemcachedStore
     */
    public MemcachedStore() {

        synchronized (MemcachedStore.class) {

            status = Status.STATUS_UNINITIALISED;

            String[] serverlist = {"localhost:12345"};
            Integer[] weights = {new Integer(5)};
            int initialConnections = 10;
            int minSpareConnections = 5;
            int maxSpareConnections = 50;
            long maxIdleTime = DEFAULT_IDLE_TIME;
            long maxBusyTime = 1000 * 60 * 5;    // 5 minutes
            long maintThreadSleep = 1000 * 5;            // 5 seconds
            int socketTimeOut = 1000 * 3;            // 3 seconds to block on reads
            int socketConnectTO = 1000 * 3;            // 3 seconds to block on initial connections.  If 0, then will use blocking connect (default)
            boolean failover = false;            // turn off auto-failover in event of server down
            boolean nagleAlg = false;            // turn off Nagle's algorithm on all sockets in pool

            socketIOPool = SockIOPool.getInstance("poolName");
            socketIOPool.setServers(serverlist);
            socketIOPool.setWeights(weights);
            socketIOPool.setInitConn(initialConnections);
            socketIOPool.setMinConn(minSpareConnections);
            socketIOPool.setMaxConn(maxSpareConnections);
            socketIOPool.setMaxIdle(maxIdleTime);
            socketIOPool.setMaxBusyTime(maxBusyTime);
            socketIOPool.setMaintSleep(maintThreadSleep);
            socketIOPool.setSocketTO(socketTimeOut);
            socketIOPool.setSocketConnectTO(socketConnectTO);
            socketIOPool.setNagle(nagleAlg);
            socketIOPool.setHashingAlg(SockIOPool.NEW_COMPAT_HASH);
            socketIOPool.setFailover(failover);

            try {
                socketIOPool.initialize();
                status = Status.STATUS_ALIVE;
            } catch (Exception e) {
                throw new CacheException("Could not initialise MemcachedStore. Cause was: " + e.getMessage(), e);
            }

        }


    }


    /**
     * Puts an item into the cache.
     */
    public void put(Element element) throws CacheException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Gets an item from the cache.
     */
    public Element get(Object key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Removes an item from the cache.
     *
     * @since signature changed in 1.2 from boolean to Element to support notifications
     */
    public Element remove(Object key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Remove all of the elements from the store.
     * <p/>
     * If there are registered <code>CacheEventListener</code>s they are notified of the expiry or removal
     * of the <code>Element</code> as each is removed.
     */
    public void removeAll() throws CacheException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Prepares for shutdown.
     */
    public void dispose() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Returns the current store size.
     */
    public int getSize() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Returns the cache status.
     */
    public Status getStatus() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * A check to see if a key is in the Store.
     *
     * @param key The Element key
     * @return true if found. No check is made to see if the Element is expired.
     *         1.2
     */
    public boolean containsKey(Object key) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
