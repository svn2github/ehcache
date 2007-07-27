package net.sf.ehcache.exceptionhandler;

import net.sf.ehcache.Ehcache;

/**
 * A handler which may be registered with an Ehcache, to handle exception on Cache operations.
 * <p/>
 * Handlers may be registered at configuration time in ehcache.xml, using a CacheExceptionHandlerFactory, or
 *  set at runtime (a strategy).
 * <p/>
 * If an exception handler is registered, the default behaviour of throwing the exception will not occur. The handler
 * method <code>onException</code> will be called. Of course, if the handler decides to throw the exception, it will
 * propagate up through the call stack. If the handler does not, it won't.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public interface ExceptionHandler {

    /**
     * Called if an Exception occurs in a Cache method. This method is not called
     * if an <code>Error</code> occurs.
     *
     * @param ehcache   the cache in which the Exception occurred
     * @param key       the key used in the operation, or null if the operation does not use a key
     * @param exception the exception caught
     */
    void onException(Ehcache ehcache, Object key, Exception exception);
}

