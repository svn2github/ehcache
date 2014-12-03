package net.sf.ehcache.terracotta;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.event.TerracottaCacheEventReplicationFactory;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class EventReplicationTest {

    @Test
    public void testConfigFileHonorsClusteringOff() {
        final CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/terracotta/ehcache-event-replication.xml"));
        final Cache cache = cacheManager.getCache("replication");
        assertThat(cache, notNullValue());
        final TerracottaConfiguration terracottaConfiguration = cache.getCacheConfiguration().getTerracottaConfiguration();
        assertThat(terracottaConfiguration, notNullValue());
        assertThat(terracottaConfiguration.isClustered(), is(false));
        final List eventListenerConfigurations = cache.getCacheConfiguration().getCacheEventListenerConfigurations();
        assertThat(eventListenerConfigurations, notNullValue());
        assertThat(eventListenerConfigurations.size(), is(1));
        assertThat(((CacheConfiguration.CacheEventListenerFactoryConfiguration)eventListenerConfigurations.get(0)).getFullyQualifiedClassPath(),
            equalTo(TerracottaCacheEventReplicationFactory.class.getName()));
        cache.put(new Element("key", "value"));
        assertThat((String) cache.get("key").getValue(), equalTo("value"));
    }
}
