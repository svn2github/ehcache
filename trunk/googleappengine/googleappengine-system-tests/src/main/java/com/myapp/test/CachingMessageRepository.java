package com.myapp.test;

import com.myapp.test.model.Message;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lorban
 */
public class CachingMessageRepository extends MessageRepository {

    private final CacheManager cacheManager = CacheManager.getInstance();
    private final Cache cache = cacheManager.getCache("messageCache");
    private boolean cacheWarm = false;

    @Override
    public Collection<Message> getAll() {
        synchronized (cache) {
            if (!cacheWarm) {
                Collection<Message> messages = super.getAll();
                for (Message message : messages) {
                    cache.put(new Element(message.getId(), message));
                }
                cacheWarm = true;
            }
        }

        List<Message> result = new ArrayList<Message>();
        List keys = cache.getKeys();
        for (Object key : keys) {
            Message value = (Message) cache.get(key).getObjectValue();
            result.add(value);
        }
        return result;
    }

    @Override
    public void create(Message message) {
        super.create(message);
        cache.put(new Element(message.getId(), message));
    }

    @Override
    public void deleteById(Long id) {
        super.deleteById(id);
        cache.remove(id);
    }
}
