package net.sf.ehcache.constructs.scheduledrefresh;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.extension.CacheExtensionFactory;

public class TestScheduledRefreshFactory extends CacheExtensionFactory {

    @Override
    public CacheExtension createCacheExtension(Ehcache cache,
            Properties properties) {
        ScheduledRefreshConfiguration conf = new ScheduledRefreshConfiguration().fromProperties(properties).build();
        return new ScheduledRefreshCacheExtension(conf, cache);
    }

}
