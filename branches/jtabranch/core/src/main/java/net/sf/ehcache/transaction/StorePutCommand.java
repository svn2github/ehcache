package net.sf.ehcache.transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;

/**
 * @author Alex Snaps
 */
public class StorePutCommand implements StoreWriteCommand {

    private final Element element;

    public StorePutCommand(final Element element) {
        this.element = element;
    }

    public void execute(final Store store) {
        store.put(element);
    }
}
