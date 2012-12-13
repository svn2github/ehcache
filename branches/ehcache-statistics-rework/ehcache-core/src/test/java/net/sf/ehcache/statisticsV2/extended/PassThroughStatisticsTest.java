/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.Test;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.number.OrderingComparison.*;
import static org.junit.Assert.assertThat;

/**
 *
 * @author cdennis
 */
public class PassThroughStatisticsTest {
    
    @Test
    public void testGetSize() {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();
            
            assertThat(extendedStats.getSize(), is(0L));
            
            foo.put(new Element("foo", "foo"));
            
            assertThat(extendedStats.getSize(), is(1L));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testGetLocalHeapSize() {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();
            
            assertThat(extendedStats.getLocalHeapSize(), is(0L));
            
            foo.put(new Element("foo", "foo"));
            
            assertThat(extendedStats.getLocalHeapSize(), is(1L));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testGetLocalHeapSizeInBytes() {
        CacheManager manager = new CacheManager(new Configuration().name("foo-manager"));
        try {
            Cache foo = new Cache(new CacheConfiguration().name("foo").maxEntriesLocalHeap(1000));
            manager.addCache(foo);
            
            ExtendedStatistics extendedStats = foo.getStatistics().getExtended();
            
            assertThat(extendedStats.getLocalHeapSize(), is(0L));
            
            foo.put(new Element("foo", "foo"));
            
            assertThat(extendedStats.getLocalHeapSizeInBytes(), greaterThan(1L));
        } finally {
            manager.shutdown();
        }
    }
}
