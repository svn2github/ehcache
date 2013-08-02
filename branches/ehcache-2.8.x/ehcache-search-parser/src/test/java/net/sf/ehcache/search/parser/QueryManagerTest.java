package net.sf.ehcache.search.parser;

import java.util.Collection;
import java.util.HashSet;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.query.QueryManager;

import org.junit.Test;

public class QueryManagerTest {
    
    private final static CacheManager newCacheManager(String cmName) {
        Configuration cmConfig = new Configuration().name(cmName);
        CacheManager cm = new CacheManager(cmConfig);
        for (int i = 0; i < 5; i++) {
            CacheConfiguration cfg = new CacheConfiguration().name("foo" + i).maxEntriesLocalHeap(10);
            cfg.addSearchable(new Searchable());
            cm.addCache(new Cache(cfg));
        }
        return cm;
    }

    @Test
    public void testSameCacheNameInMultipleCacheManagers() {
        CacheManager cm1 = newCacheManager("cm1");
        CacheManager cm2 = newCacheManager("cm2");
        
        Collection<Ehcache> caches = new HashSet<Ehcache>();
        String[] cm1Caches = cm1.getCacheNames();
        String[] cm2Caches = cm2.getCacheNames();
        
        for (String c: cm1Caches) {
            caches.add(cm1.getEhcache(c));
        }
            
        for (String c: cm2Caches) {
            caches.add(cm2.getEhcache(c));
        }

        QueryManager qm = new QueryManagerImpl(caches);
        qm.createQuery("select key from " + cm2Caches[0]);
    }
    
}
