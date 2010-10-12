package com.myapp.test;

import com.myapp.test.model.Message;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lorban
 */
public class CachingMessageRepository extends MessageRepository {

    private static final Logger log = LoggerFactory.getLogger(CachingMessageRepository.class);


    private static MessageRepository instance = null;

    private final CacheManager cacheManager = CacheManager.getInstance();
    private final Cache cache = cacheManager.getCache("messageCache");

    private CachingMessageRepository() {
    }


    @Override
    public void create(Message message) {
        super.create(message);
        cache.put(new Element(message.getId(), message));
    }

    @Override
    public Message getById(Long id) {
        Element element = cache.getWithLoader(id, null, null);
        Message message;
        if (element != null) {
            if (log.isDebugEnabled()) log.debug("loading object from cache: " + id);
            message = (Message) element.getObjectValue();
        }
        else {
            if (log.isDebugEnabled()) log.debug("loading object from datastore: " + id);
            message = super.getById(id);
            cache.put(new Element(id, message));
        }
        return message;
    }

    @Override
    public void deleteById(Long id) {
        super.deleteById(id);
        cache.remove(id);
    }


    public synchronized static MessageRepository get() {
        if (instance == null) {
            instance = new CachingMessageRepository();
        }
        return instance;
    }
}
