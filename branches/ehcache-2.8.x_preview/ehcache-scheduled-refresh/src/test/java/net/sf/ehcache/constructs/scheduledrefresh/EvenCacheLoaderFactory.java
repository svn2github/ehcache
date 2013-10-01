package net.sf.ehcache.constructs.scheduledrefresh;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CacheLoaderFactory;

import java.util.Properties;

public class EvenCacheLoaderFactory extends CacheLoaderFactory {

    @Override
    public CacheLoader createCacheLoader(Ehcache cache, Properties properties) {
        return new EvenCacheLoader();
    }

}
