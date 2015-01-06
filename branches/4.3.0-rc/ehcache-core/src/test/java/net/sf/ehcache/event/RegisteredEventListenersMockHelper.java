package net.sf.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * This class exists solely to widen the scope of the 5 internal package protected methods in
 * {@link net.sf.ehcache.event.RegisteredEventListeners} so that it can be mocked.
 *
 * @author cschanck
 */
public class RegisteredEventListenersMockHelper extends RegisteredEventListeners {

    public RegisteredEventListenersMockHelper(Cache cache) {
        super(cache);
    }

    public RegisteredEventListenersMockHelper(Ehcache cache, CacheStoreHelper helper) {
        super(cache, helper);
    }

    @Override
    public void internalNotifyElementRemoved(Element element, ElementCreationCallback callback, boolean remoteEvent) {
        super.internalNotifyElementRemoved(element, callback, remoteEvent);
    }

    @Override
    public void internalNotifyElementPut(Element element, ElementCreationCallback callback, boolean remoteEvent) {
        super.internalNotifyElementPut(element, callback, remoteEvent);
    }

    @Override
    public void internalNotifyElementUpdated(Element element, ElementCreationCallback callback, boolean remoteEvent) {
        super.internalNotifyElementUpdated(element, callback, remoteEvent);
    }

    @Override
    public void internalNotifyElementExpiry(Element element, ElementCreationCallback callback, boolean remoteEvent) {
        super.internalNotifyElementExpiry(element, callback, remoteEvent);
    }

    @Override
    public void internalNotifyElementEvicted(Element element, ElementCreationCallback callback, boolean remoteEvent) {
        super.internalNotifyElementEvicted(element, callback, remoteEvent);
    }
}
