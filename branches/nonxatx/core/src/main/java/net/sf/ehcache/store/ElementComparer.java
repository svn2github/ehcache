package net.sf.ehcache.store;

import net.sf.ehcache.Element;

/**
 * @author Ludovic Orban
 */
public interface ElementComparer {

    boolean fullElementEquals(Element e1, Element e2);

}
