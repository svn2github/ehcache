package net.sf.ehcache.search;

import net.sf.ehcache.CacheException;

/**
 * Generic search exception. This exception (or a more specific subclass) will be
 * thrown for a number of conditions including (but not limited to):
 * <ul>
 * <li>Type conflict for search attribute. For example a search attribute is of
 * type "int" but the query criteria is for equals("some string")
 * <li>IOException or timeout communicating with a remote server or performing
 * disk I/O
 * <li>Attempting to read from a discard()'d {@link Results} instance
 *
 * @author teck
 */
public class SearchException extends CacheException {

    private static final long serialVersionUID = 6942653724476318512L;

    public SearchException(String message) {
        super(message);
    }
}
