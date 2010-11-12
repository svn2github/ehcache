package net.sf.ehcache.config;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;

/**
 * @author Ludovic Orban
 */
public class FakeElementValueComparator implements ElementValueComparator {
    public boolean equals(Element e1, Element e2) {
        return false;
    }
}
