package net.sf.ehcache.transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;

/**
 * @author Alex Snaps
 */
public class StoreRemoveCommand implements VersionAwareStoreWriteCommand {

    private final Object key;
    private final Element element;

    public StoreRemoveCommand(final Object key, Element element) {
        this.key = key;
        this.element = element;
    }

    public void execute(final Store store) {
        store.remove(key);
    }
    
    public Element getElement() {
        return element;
    }
}
