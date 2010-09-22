package net.sf.ehcache.search.attribute;

import java.io.Serializable;

import net.sf.ehcache.Element;

/**
 * Used to extract a search attribute value for a given cache element.<br>
 * <br>
 * Instances must be {@link Serializable} in order to ensure identical
 * extractors are used in distributed caches
 *
 * @author teck
 */
public interface AttributeExtractor extends Serializable {
    /**
     * Extract the attribute value. The instance returned from this method must
     * be one of:
     * <ul>
     * <li>java.lang.Boolean
     * <li>java.lang.Byte
     * <li>java.lang.Character
     * <li>java.lang.Double
     * <li>java.lang.Float
     * <li>java.lang.Integer
     * <li>java.lang.Long
     * <li>java.lang.Short
     * <li>java.lang.String
     * <li>java.util.Date
     * <li>a java enum
     * </ul>
     * <p/>
     * NOTE: null is a legal return here as well indicating that this attribute
     * will not be available for the given element
     *
     * @param element the cache element to inspect
     * @return the attribute value
     */
    Object attributeFor(Element element);
}
