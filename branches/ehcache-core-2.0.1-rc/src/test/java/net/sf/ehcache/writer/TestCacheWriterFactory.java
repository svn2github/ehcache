package net.sf.ehcache.writer;

import net.sf.ehcache.Ehcache;

import java.util.Properties;

public class TestCacheWriterFactory extends CacheWriterFactory {
    @Override
    public CacheWriter createCacheWriter(Ehcache cache, Properties properties) {
        return new TestCacheWriter(properties);
    }
}