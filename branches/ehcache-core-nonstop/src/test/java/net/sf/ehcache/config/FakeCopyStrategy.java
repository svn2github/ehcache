package net.sf.ehcache.config;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;

/**
 * @author Alex Snaps
 */
public class FakeCopyStrategy implements ReadWriteCopyStrategy<Element> {

    public Element copyForWrite(Element value) {
        return null;
    }

    public Element copyForRead(Element storedValue) {
        return null;
    }
}
