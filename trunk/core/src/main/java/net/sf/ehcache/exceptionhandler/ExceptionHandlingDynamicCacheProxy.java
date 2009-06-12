/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.exceptionhandler;

import net.sf.ehcache.Ehcache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * A dynamic proxy which provides CacheException handling.
 * <p/>
 * The ehcache configuration will create and register in the <code>CacheManager</code> {@link Ehcache}s decorated
 * with this dynamic proxy. See following for programmatic use.
 * <p/>
 * The createProxy factory method may be used to simply create a proxy. Otherwise the calling client
 * will need code similar to:
 * <pre>
 * (Ehcache) Proxy.newProxyInstance(ehcache.getClass().getClassLoader(), new Class[]{ Ehcache.class },
 * new ExceptionHandlingDynamicCacheProxy(ehcache));</pre>
 * <p/>
 * A common usage is to create a proxy and then register the proxy in <code>CacheManager</code> in place of the
 * underlying cache. To do that create a proxy and then call
 * <pre>
 * cacheManager.replaceCacheWithDecoratedCache(Ehcache cache, Ehcache decoratedCache);
 * </pre>
 * All clients accessing the cache through<code>cacheManager.getEhcache()</code> will then receive proxy references.
 * <p/>
 * See CacheTest for a perf test.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public final class ExceptionHandlingDynamicCacheProxy implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlingDynamicCacheProxy.class.getName());

    private Ehcache ehcache;

    /**
     * Constructor: Use with something like:
     * <pre>
     * (Ehcache) Proxy.newProxyInstance(ehcache.getClass().getClassLoader(), new Class[]{ Ehcache.class },
     * new ExceptionHandlingDynamicCacheProxy(ehcache));</pre>
     * @param ehcache the backing ehcache
     */
    public ExceptionHandlingDynamicCacheProxy(Ehcache ehcache) {
        this.ehcache = ehcache;
    }

    /**
     * A simple factory method to hide the messiness of creating the proxy from clients.
     *
     * @param ehcache the target cache
     * @return a proxied Ehcache
     */
    public static Ehcache createProxy(Ehcache ehcache) {
        return (Ehcache) Proxy.newProxyInstance(ehcache.getClass().getClassLoader(), new Class[]{Ehcache.class},
                new ExceptionHandlingDynamicCacheProxy(ehcache));
    }



    /**
     * Processes a method invocation on a proxy instance and returns
     * the result.  This method will be invoked on an invocation handler
     * when a method is invoked on a proxy instance that it is
     * associated with.
     *
     * @param proxy  the proxy instance that the method was invoked on
     * @param method the <code>Method</code> instance corresponding to
     *               the interface method invoked on the proxy instance.  The declaring
     *               class of the <code>Method</code> object will be the interface that
     *               the method was declared in, which may be a superinterface of the
     *               proxy interface that the proxy class inherits the method through.
     * @param args   an array of objects containing the values of the
     *               arguments passed in the method invocation on the proxy instance,
     *               or <code>null</code> if interface method takes no arguments.
     *               Arguments of primitive types are wrapped in instances of the
     *               appropriate primitive wrapper class, such as
     *               <code>java.lang.Integer</code> or <code>java.lang.Boolean</code>.
     * @return the value to return from the method invocation on the
     *         proxy instance.  If the declared return type of the interface
     *         method is a primitive type, then the value returned by
     *         this method must be an instance of the corresponding primitive
     *         wrapper class; otherwise, it must be a type assignable to the
     *         declared return type.  If the value returned by this method is
     *         <code>null</code> and the interface method's return type is
     *         primitive, then a <code>NullPointerException</code> will be
     *         thrown by the method invocation on the proxy instance.  If the
     *         value returned by this method is otherwise not compatible with
     *         the interface method's declared return type as described above,
     *         a <code>ClassCastException</code> will be thrown by the method
     *         invocation on the proxy instance.
     * @throws Throwable the exception to throw from the method
     *                   invocation on the proxy instance.  The exception's type must be
     *                   assignable either to any of the exception types declared in the
     *                   <code>throws</code> clause of the interface method or to the
     *                   unchecked exception types <code>java.lang.RuntimeException</code>
     *                   or <code>java.lang.Error</code>.  If a checked exception is
     *                   thrown by this method that is not assignable to any of the
     *                   exception types declared in the <code>throws</code> clause of
     *                   the interface method, then an
     *                   {@link java.lang.reflect.UndeclaredThrowableException} containing the
     *                   exception that was thrown by this method will be thrown by the
     *                   method invocation on the proxy instance.
     * @see java.lang.reflect.UndeclaredThrowableException
     *
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object invocationResult = null;
        try {
            invocationResult = method.invoke(ehcache, args);
        } catch (Exception e) {
            CacheExceptionHandler cacheExceptionHandler = ehcache.getCacheExceptionHandler();
            if (cacheExceptionHandler != null) {
                String keyAsString = null;
                //should be a CacheException
                Throwable cause = e.getCause();
                if (cause != null) {
                    keyAsString = extractKey(cause.getMessage());
                }
                Exception causeAsException = null;
                try {
                    causeAsException = (Exception) cause;
                } catch (ClassCastException cce) {
                    //we only handle exceptions, not errors.
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Underlying cause was not an Exception: " + cce);
                    }
                }

                cacheExceptionHandler.onException(ehcache, keyAsString, causeAsException);
            } else {
                throw e.getCause();
            }

        }
        return invocationResult;
    }

    /**
     * Extracts the key from the message, if any
     */
    static String extractKey(String message) {
        if (message == null) {
            return null;
        }
        int beginIndex = message.lastIndexOf("key ");
        if (beginIndex < 0) {
            return null;
        }
        beginIndex += "key ".length();
        int endIndex = beginIndex;
        for (int i = beginIndex; i < message.length(); i++) {
            char character = message.charAt(i);
            if (character == ' ') {
                break;
            }
            endIndex = i;
        }
        endIndex++;
        if (endIndex > message.length()) {
            endIndex = message.length();
        }
        String key = message.substring(beginIndex, endIndex);
        return key;
    }
}
