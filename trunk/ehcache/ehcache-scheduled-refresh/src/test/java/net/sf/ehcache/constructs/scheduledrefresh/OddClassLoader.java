package net.sf.ehcache.constructs.scheduledrefresh;

public class OddClassLoader extends IncrementingCacheLoader {

    public OddClassLoader() {
        super(false,10000);
    }
    
}