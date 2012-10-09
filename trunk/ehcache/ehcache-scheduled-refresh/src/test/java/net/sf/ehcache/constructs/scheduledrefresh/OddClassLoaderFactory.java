package net.sf.ehcache.constructs.scheduledrefresh;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CacheLoaderFactory;

public class OddClassLoaderFactory extends CacheLoaderFactory {

    @Override
    public CacheLoader createCacheLoader(Ehcache cache, Properties properties) {
        return new OddClassLoader();
    }

}
