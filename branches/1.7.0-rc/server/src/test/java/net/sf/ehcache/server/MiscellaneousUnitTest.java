package net.sf.ehcache.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * This is a parking space to try out ideas that _may_ go somewhere
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class MiscellaneousUnitTest {

    /**
     * Tests a manual cluster partitioning technique based on key hashing
     * i.e. assuming 5 clusters behind a load balancer named cluster1, cluster2...
     * Each cluster could have 2 or more ehcache servers in it responding to queries and distributing amongst other nodes in their cluster.
     */
    @Test
    public void testHashing() {

        String[] cacheservers = new String[]{"cacheserver0.company.com", "cacheserver1.company.com", "cacheserver2.company.com", "cacheserver3.company.com",
                "cacheserver4.company.com", "cacheserver5.company.com"};

        net.sf.ehcache.Element element = new net.sf.ehcache.Element("dfadfasdfadsfa", "some value");
        Object key = element.getKey();
        int hash = Math.abs(key.hashCode());
        int cacheserverIndex = hash % cacheservers.length;

        String cacheserverToUseForKey = cacheservers[cacheserverIndex];

        assertEquals("cacheserver4.company.com", cacheserverToUseForKey);
    }


    @Test
    public void testByteArrayEquality() throws NoSuchAlgorithmException {

        byte[] bytes1 = new byte[]{1,2,3};
        byte[] bytes2 = new byte[]{1,2,3};
        byte[] bytes3 = bytes1;
        
        //cannot compare using equals. It uses ==
        assertFalse(bytes1 == bytes2);
        assertTrue(bytes1 == bytes3);


        assertTrue(bytes1.length == bytes2.length);

        boolean equals = Arrays.equals(bytes1, bytes2);
        assertTrue(equals);


    }
}




