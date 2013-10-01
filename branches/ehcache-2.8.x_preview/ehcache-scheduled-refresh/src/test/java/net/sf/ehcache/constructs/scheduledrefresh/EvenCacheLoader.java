package net.sf.ehcache.constructs.scheduledrefresh;

public class EvenCacheLoader extends IncrementingCacheLoader {

    public EvenCacheLoader() {
        super(true, 20000);
    }

}