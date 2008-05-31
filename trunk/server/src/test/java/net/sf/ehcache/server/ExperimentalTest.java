package net.sf.ehcache.server;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * This is a parking space to try out ideas that _may_ go somewhere
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class ExperimentalTest {

    /**
     * Tests a manual cluster partitioning technique based on key hashing
     * i.e. assuming 5 clusters behind a load balancer named cluster1, cluster2...
     * Each cluster could have 2 or more ehcache servers in it responding to queries and distributing amongst other nodes in their cluster.
     */
    @Test
    public void testHashing() {

        String[] clusters = new String[]{"cluster0.company.com", "cluster1.company.com", "cluster2.company.com", "cluster3.company.com",
                "cluster4.company.com", "cluster5.company.com"};

        net.sf.ehcache.Element element = new net.sf.ehcache.Element("dfadfasdfadsfa", "some value");
        Object key = element.getKey();
        int hash = Math.abs(key.hashCode());
        int clusterIndex = hash % clusters.length;

        String clusterToUseForKey = clusters[clusterIndex];

        assertEquals("cluster4.company.com", clusterToUseForKey);
    }
}
