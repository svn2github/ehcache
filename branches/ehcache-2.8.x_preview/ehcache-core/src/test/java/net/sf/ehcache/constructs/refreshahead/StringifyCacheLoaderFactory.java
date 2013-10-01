package net.sf.ehcache.constructs.refreshahead;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CacheLoaderFactory;

public class StringifyCacheLoaderFactory extends CacheLoaderFactory {

    @Override
    public CacheLoader createCacheLoader(Ehcache cache, Properties properties) {
        return new StringifyCacheLoader(properties);
    }

    public static class StringifyCacheLoader implements CacheLoader {

        private long delayMS = 0;

        public StringifyCacheLoader(Properties properties) {
            if (properties != null) {
                delayMS = Long.parseLong(properties.getProperty("delayMS", "0"));
            }
        }

        @Override
        public Map loadAll(Collection keys, Object argument) {
            return loadAll(keys);
        }

        @Override
        public Map loadAll(Collection keys) {
            try {
                Thread.sleep(delayMS);
            } catch (InterruptedException e) {

            }
            HashMap ret=new HashMap();
            for(Object k:keys) {
                ret.put(k, k.toString());
            }
            return ret;
        }

        @Override
        public Object load(Object key, Object argument) {
            return load(key);
        }

        @Override
        public Object load(Object key) throws CacheException {
            try {
                Thread.sleep(delayMS);
            } catch (InterruptedException e) {

            }
            return key.toString();
        }

        @Override
        public void init() {
            // TODO Auto-generated method stub

        }

        @Override
        public Status getStatus() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void dispose() throws CacheException {
            // TODO Auto-generated method stub

        }

        @Override
        public CacheLoader clone(Ehcache cache) throws CloneNotSupportedException {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
