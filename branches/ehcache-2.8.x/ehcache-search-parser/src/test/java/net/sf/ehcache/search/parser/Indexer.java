package net.sf.ehcache.search.parser;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeExtractorException;

 
/**
 * @author Alex Snaps
 */
public class Indexer implements AttributeExtractor {

  /**
   * 
   */
  private static final long serialVersionUID = 8759323054127235182L;

  public final Object attributeFor(final Element element, final String attributeName) throws AttributeExtractorException {
    final Object objectValue = element.getObjectValue();
    if (objectValue instanceof CacheValue) {
      return ((CacheValue)objectValue).getValue(attributeName);
    }
    return retrieveFromUnknownType(element, attributeName);
  }

  protected Object retrieveFromUnknownType(final Element element, final String attributeName) {
    return null;
  }
}