package net.sf.ehcache.search.attribute;

import net.sf.ehcache.search.SearchException;

import java.io.Serializable;

/**
 * An exception to indicate that an attribute extractor was unable to be processed.
 * <p/>
 * Attributes are extracted on put or update, so this exception will be thrown to the calling
 * thread.
 * 
 * @author Greg Luck
 */
public class AttributeExtractorException extends SearchException implements Serializable {


    private static final long serialVersionUID = 5066522240394222152L;


    /**
     * Construct a AttributeExtractorException
     *
     * @param message the description of the exception
     */
    public AttributeExtractorException(String message) {
        super(message);
    }

    /**
     * Construct a AttributeExtractorException with an underlying cause
     *
     * @param message
     * @param cause
     */
    public AttributeExtractorException(String message, Throwable cause) {
        super(message, cause);
    }
}
