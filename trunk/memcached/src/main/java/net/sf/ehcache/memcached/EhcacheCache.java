package net.sf.ehcache.memcached;

import com.thimbleware.jmemcached.Cache;
import com.thimbleware.jmemcached.CacheElement;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.LocalCacheElement;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class EhcacheCache implements Cache<CacheElement> {

    private final static Logger LOG = LoggerFactory.getLogger(EhcacheCache.class);

    private final Ehcache cache;

    public EhcacheCache(Ehcache cache) {
        this.cache = cache;
    }

    public DeleteResponse delete(Key key, int time) {
        EhcacheKey ehcacheKey = new EhcacheKey(key);
        boolean removed = cache.remove(ehcacheKey.toKey());
        LOG.debug("{}: delete {} -> {}", new Object[] {cache.getName(), ehcacheKey, removed});
        return removed ? DeleteResponse.DELETED : DeleteResponse.NOT_FOUND;
    }

    public StoreResponse add(CacheElement e) {
        EhcacheKey key = new EhcacheKey(e.getKey());
        EhcacheValue value = new EhcacheValue(e.getData());

        Element cachedElement = cache.putIfAbsent(new Element(key.toKey(), value.toValue()));
        LOG.debug("{}: add {} -> {}", new Object[] {cache.getName(), key, cachedElement == null});
        return cachedElement == null ? StoreResponse.STORED : StoreResponse.EXISTS;
    }

    public StoreResponse replace(CacheElement e) {
        EhcacheKey key = new EhcacheKey(e.getKey());
        EhcacheValue value = new EhcacheValue(e.getData());

        Element cachedElement = cache.replace(new Element(key.toKey(), value.toValue()));
        LOG.debug("{}: replace {} -> {}", new Object[] {cache.getName(), key, cachedElement == null});
        return cachedElement == null ? StoreResponse.NOT_FOUND : StoreResponse.STORED;
    }

    public StoreResponse append(CacheElement cacheElement) {
        return null;
    }

    public StoreResponse prepend(CacheElement cacheElement) {
        return null;
    }

    public StoreResponse set(CacheElement e) {
        EhcacheKey key = new EhcacheKey(e.getKey());
        EhcacheValue value = new EhcacheValue(e.getData());

        cache.put(new Element(key.toKey(), value.toValue()));
        LOG.debug("{}: set {} to {} -> true", new Object[] {cache.getName(), key, value});

        return StoreResponse.STORED;
    }

    public StoreResponse cas(Long cas_key, CacheElement e) {
        return null;
    }

    public Integer get_add(Key key, int mod) {
        return null;
    }

    public CacheElement[] get(Key... keys) {
        List<CacheElement> cacheElements = new ArrayList<CacheElement>();

        for (Key key : keys) {
            Element element = cache.get(new EhcacheKey(key).toKey());
            if (element == null) {
                LOG.debug("{}: get {} -> {}", new Object[] {cache.getName(), new EhcacheKey(key), null});
                continue;
            }

            Key localCacheKey;
            Object objectKey = element.getObjectKey();
            if (objectKey instanceof EhcacheKey) {
                EhcacheKey ehcacheKey = (EhcacheKey) objectKey;
                localCacheKey = ehcacheKey.getKey();
            } else {
                String s = (String) objectKey;
                try {
                    localCacheKey = new Key(new BigEndianHeapChannelBuffer(s.getBytes("utf-8")));
                } catch (UnsupportedEncodingException e) {
                    throw new CacheException("unsupported utf-8 encoding", e);
                }
            }

            ChannelBuffer localCacheData;
            Object objectValue = element.getObjectValue();
            if (objectValue instanceof EhcacheValue) {
                EhcacheValue ehcacheValue = (EhcacheValue) objectValue;
                localCacheData = ehcacheValue.getData();
            } else {
                String s = (String) objectValue;
                try {
                    localCacheData = new BigEndianHeapChannelBuffer(s.getBytes("utf-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new CacheException("unsupported utf-8 encoding", e);
                }
            }

            LOG.debug("{}: get {} -> {}", new Object[] {cache.getName(), new EhcacheKey(localCacheKey), new EhcacheValue(localCacheData)});

            LocalCacheElement localCacheElement = new LocalCacheElement(localCacheKey);
            localCacheElement.setData(localCacheData);

            cacheElements.add(localCacheElement);
        }

        return cacheElements.toArray(new CacheElement[cacheElements.size()]);
    }
/*
    public CacheElement[] get(Key... keys) {
        List<CacheElement> cacheElements = new ArrayList<CacheElement>();

        for (Key key : keys) {
            Element element = cache.get(new EhcacheKey(key));
            if (element == null) {
                LOG.debug("{}: get {} -> {}", new Object[] {cache.getName(), new EhcacheKey(key), null});
                continue;
            }

            Object objectKey = element.getObjectKey();
            EhcacheKey ehcacheKey = (EhcacheKey) objectKey;
            EhcacheValue ehcacheValue = (EhcacheValue) element.getObjectValue();

            LOG.debug("{}: get {} -> {}", new Object[] {cache.getName(), ehcacheKey, ehcacheValue});

            LocalCacheElement localCacheElement = new LocalCacheElement(ehcacheKey.getKey());
            localCacheElement.setData(ehcacheValue.getData());

            cacheElements.add(localCacheElement);
        }

        return cacheElements.toArray(new CacheElement[cacheElements.size()]);
    }
*/

    public boolean flush_all() {
        cache.removeAll();
        LOG.debug("{}: flush_all -> true", cache.getName());
        return true;
    }

    public boolean flush_all(int expire) {
        cache.removeAll();
        LOG.debug("{}: flush_all -> true", cache.getName());
        return true;
    }

    public void close() throws IOException {
    }

    public long getCurrentItems() {
        return cache.getSize();
    }

    public long getLimitMaxBytes() {
        return -1;
    }

    public long getCurrentBytes() {
        return -1;
    }

    public int getGetCmds() {
        return 0;
    }

    public int getSetCmds() {
        return 0;
    }

    public int getGetHits() {
        return (int) cache.getStatistics().getCacheHits();
    }

    public int getGetMisses() {
        return (int) cache.getStatistics().getCacheMisses();
    }

    public Map<String, Set<String>> stat(String arg) {
        return new HashMap<String, Set<String>>();
    }

    public void asyncEventPing() {
    }
}
