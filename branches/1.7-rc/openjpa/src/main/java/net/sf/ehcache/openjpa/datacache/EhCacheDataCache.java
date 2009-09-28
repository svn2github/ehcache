package net.sf.ehcache.openjpa.datacache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.openjpa.datacache.AbstractDataCache;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.datacache.DataCachePCData;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.OpenJPAId;

/**
 * A plug-in {@link DataCache L2 Cache} for OpenJPA to use EhCache.
 * 
 * This cache maintains a set of Ehcaches per-class basis.
 * The name of the NamedCache for a persistence class C is determined by the 
 * {@link org.apache.openjpa.persistence.DataCache @DataCache} annotation in
 * the class C. If no name is specified in @DataCache annotation then a 
 * default name is used. The default name is <code>openjpa</code> but can be
 * configured via this plug-in's <code>DefaultName</code> property unless
 * <code>UseDefaultForUnnamedCaches</code> is set to <code>false</code>.
 * 
 * 
 * 
 * @author Pinaki Poddar
 * @author Craig Andrews
 *
 */
public class EhCacheDataCache extends AbstractDataCache implements DataCache {
    protected final Map<Class, Ehcache> _caches = new HashMap<Class, Ehcache>();
    protected boolean useDefaultForUnnamedCaches = true;
    protected String defaultName = "openjpa";
    protected ReentrantLock lock = new ReentrantLock();
    protected static final Localizer _loc = Localizer.forPackage(EhCacheDataCache.class);
    
    /**
     * Asserts if default name will be used for the Ehcache for classes
     * which do not specify explicitly a name in its @DataCache annotation.
     * The default value for this flag is <code>true</code> 
     */
    public boolean isUseDefaultForUnnamedCaches() {
        return useDefaultForUnnamedCaches;
    }

    /**
     * Sets if default name will be used for the Ehcache for classes
     * which do not specify explicitly a name in its @DataCache annotation.
     * The default value for this flag is <code>true</code> 
     */
    public void setUseDefaultForUnnamedCaches(boolean flag) {
        this.useDefaultForUnnamedCaches = flag;
    }

    /**
     * Gets the default name for the Ehcache used for classes
     * which do not specify explicitly a name in its @DataCache annotation.
     * The default name is <code>openjpa</code> 
     */
    public String getDefaultName() {
        return defaultName;
    }

    /**
     * Sets the default name for the Ehcache used for classes
     * which do not specify explicitly a name in its @DataCache annotation.
     * The default name is <code>openjpa</code> 
     */
    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }
        
    @Override
    protected void clearInternal() {
        for (Ehcache cache:_caches.values())
            cache.removeAll();
    }

    @Override
    protected DataCachePCData getInternal(Object oid) {
        Element result = null;
        if (oid instanceof OpenJPAId) {
            Class cls = ((OpenJPAId)oid).getType();
            Ehcache cache = findCache(cls);
            if (cache == null){
            	return null;
            }else{
                result = cache.get(oid);
            }
        } else {
            for (Ehcache cache:_caches.values()) {
            	result = cache.get(oid);
            	if(result!=null) break;
            }
        }
        if(result==null)
        	return null;
        else
        	return (DataCachePCData)result.getObjectValue();
    }

    @Override
    protected boolean pinInternal(Object oid) {
        return false;
    }

    @Override
    protected DataCachePCData putInternal(Object oid, DataCachePCData pc) {
        Ehcache cache = findCache(pc.getType());
        if (cache != null) {
            cache.put(new Element(oid, pc));
        }
        return pc;
    }

    @Override
    protected void removeAllInternal(Class cls, boolean subclasses) {
        for (Class c:_caches.keySet())
            if (c == cls)
                _caches.get(cls).removeAll();

    }

    @Override
    protected DataCachePCData removeInternal(Object oid) {
    	DataCachePCData result = getInternal(oid);
        Class cls = determineClassFromObjectId(oid);
        if (_caches.containsKey(cls))
        	_caches.get(cls).remove(oid);
        return result;
    }

    @Override
    protected boolean unpinInternal(Object oid) {
        return false;
    }

    public void writeLock() {
        lock.lock();
    }

    public void writeUnlock() {
        lock.unlock();
    }

    /**
     * Find an Ehcache for the given Class.
     * Makes all the following attempt in order to find a cache and if every 
     * attempt fails returns null:
     * 
     * <LI>NamedCache for the given class has been obtained before
     * <LI>Meta-data for the given class annotated for a 
     * {@link org.apache.openjpa.persistence.DataCache DataCache}. 
     * <LI>{@link #setUseDefaultForUnnamedCaches(boolean) Configured} to use
     * default cache.
     * 
     */
    protected Ehcache findCache(Class cls) {
    	Ehcache cache = _caches.get(cls);
        if (cache == null) {
            ClassMetaData meta = conf.getMetaDataRepositoryInstance()
                                     .getCachedMetaData(cls);
            String name = null;
            if (meta != null)
                name = meta.getDataCacheName();
            if (name == null || name.equals("default") || isUseDefaultForUnnamedCaches())
                name = getDefaultName();
            
            cache = CacheManager.getInstance().getEhcache(name);
            if(cache==null){
            	cache = getOrCreateCache(name);
            }
            
            //if (cache != null) {
                _caches.put(cls, cache);
            /*} else if (name == null) {
                throw new UserException(_loc.get("no-cache-name", cls));
            } else {
                throw new UserException(_loc.get("no-cache", cls, name));
            }*/
        }
        return cache;
    }
    
    protected synchronized Ehcache getOrCreateCache(String name){
    	CacheManager cacheManager = CacheManager.getInstance();
        Ehcache ehCache = cacheManager.getEhcache(name);
        if(ehCache==null){
        	Cache cache = new Cache(name,1000,false,true,600,600);
        	cacheManager.addCache(cache);
        	ehCache = cacheManager.getEhcache(name);
        }
        return ehCache;
    }
    
    protected Class determineClassFromObjectId(Object oid) {
        if (oid instanceof OpenJPAId)
            return ((OpenJPAId)oid).getType();
        return null;
    }
}
