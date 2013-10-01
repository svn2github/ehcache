package net.sf.ehcache.constructs.scheduledrefresh;

public class OddCacheLoader extends IncrementingCacheLoader {

    public OddCacheLoader() {
        super(false, 10000);
    }

}