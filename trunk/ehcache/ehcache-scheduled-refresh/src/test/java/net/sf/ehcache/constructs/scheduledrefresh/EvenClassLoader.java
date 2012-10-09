package net.sf.ehcache.constructs.scheduledrefresh;

public class EvenClassLoader extends IncrementingCacheLoader {

    public EvenClassLoader() {
        super(true,20000);
    }
    
}