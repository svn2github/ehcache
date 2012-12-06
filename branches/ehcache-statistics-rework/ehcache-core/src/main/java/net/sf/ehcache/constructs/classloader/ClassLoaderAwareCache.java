/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.constructs.classloader;

import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.statisticsV2.StatisticsPlaceholder;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * A cache decorator that adjusts the Thread context classloader (TCCL) for every cache operation. The TCCL is reset to its original value
 * when the method is complete
 *
 * @author teck
 */
public class ClassLoaderAwareCache implements Ehcache {

    /**
     * Used by InternalClassLoaderAwareCache
     */
    protected final ClassLoader classLoader;

    /**
     * Used by InternalClassLoaderAwareCache
     */
    protected final Ehcache cache;

    /**
     * Constructor
     *
     * @param cache wrapped cache
     * @param classLoader loader to set Thread context loader to for duration of cache opeartion
     */
    public ClassLoaderAwareCache(Ehcache cache, ClassLoader classLoader) {
        this.cache = cache;
        this.classLoader = classLoader;
    }

    /**
     * Generator for the method bodies
     *
     * @param args
     */
    public static void main(String[] args) {
        PrintStream out = System.out;

        for (Method m : Ehcache.class.getMethods()) {
            out.println("/**");
            out.println("* {@inheritDoc}");
            out.println("*/");
            out.print("public " + m.getReturnType().getSimpleName() + " " + m.getName() + "(");
            Class<?>[] params = m.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                out.print(params[i].getSimpleName() + " arg" + i);
                if (i < params.length - 1) {
                    out.print(", ");
                }
            }
            out.print(")");

            Class<?>[] exceptions = m.getExceptionTypes();
            if (exceptions.length > 0) {
                out.print(" throws ");
            }
            for (int i = 0; i < exceptions.length; i++) {
                out.print(exceptions[i].getSimpleName());
                if (i < exceptions.length - 1) {
                    out.print(", ");
                }
            }

            out.println(" {");
            out.println("    // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!");
            out.println("    Thread t = Thread.currentThread();");
            out.println("    ClassLoader prev = t.getContextClassLoader();");
            out.println("    t.setContextClassLoader(this.classLoader);");
            out.println("    try {");
            out.print("        ");
            if (m.getReturnType() != Void.TYPE) {
                out.print("return ");
            }
            out.print("this.cache." + m.getName() + "(");
            for (int i = 0; i < params.length; i++) {
                out.print("arg" + i);
                if (i < params.length - 1) {
                    out.print(", ");
                }
            }
            out.println(");");
            out.println("    } finally {");
            out.println("        t.setContextClassLoader(prev);");
            out.println("    }");
            out.println("}");
            out.println("");
        }
    }

    /**
    * {@inheritDoc}
    */
    public void putQuiet(Element arg0) throws IllegalArgumentException, IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.putQuiet(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void putWithWriter(Element arg0) throws IllegalArgumentException, IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.putWithWriter(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Map getAll(Collection arg0) throws IllegalStateException, CacheException, NullPointerException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getAll(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Element getQuiet(Serializable arg0) throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getQuiet(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Element getQuiet(Object arg0) throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getQuiet(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getKeysWithExpiryCheck();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getKeysNoDuplicateCheck();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean removeQuiet(Serializable arg0) throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.removeQuiet(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean removeQuiet(Object arg0) throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.removeQuiet(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean removeWithWriter(Object arg0) throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.removeWithWriter(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean hasAbortedSizeOf() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.hasAbortedSizeOf();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isExpired(Element arg0) throws IllegalStateException, NullPointerException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isExpired(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public RegisteredEventListeners getCacheEventNotificationService() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getCacheEventNotificationService();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isElementInMemory(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isElementInMemory(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isElementInMemory(Serializable arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isElementInMemory(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isElementOnDisk(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isElementOnDisk(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isElementOnDisk(Serializable arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isElementOnDisk(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public String getGuid() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getGuid();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public CacheManager getCacheManager() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getCacheManager();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void evictExpiredElements() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.evictExpiredElements();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isKeyInCache(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isKeyInCache(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isValueInCache(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isValueInCache(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public StatisticsPlaceholder getStatistics() throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getStatistics();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void setCacheManager(CacheManager arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setCacheManager(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public BootstrapCacheLoader getBootstrapCacheLoader() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getBootstrapCacheLoader();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void setBootstrapCacheLoader(BootstrapCacheLoader arg0) throws CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setBootstrapCacheLoader(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void initialise() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.initialise();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void bootstrap() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.bootstrap();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public CacheConfiguration getCacheConfiguration() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getCacheConfiguration();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void registerCacheExtension(CacheExtension arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.registerCacheExtension(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void unregisterCacheExtension(CacheExtension arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.unregisterCacheExtension(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public List getRegisteredCacheExtensions() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getRegisteredCacheExtensions();
        } finally {
            t.setContextClassLoader(prev);
        }
    }


    /**
    * {@inheritDoc}
    */
    public void setCacheExceptionHandler(CacheExceptionHandler arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setCacheExceptionHandler(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public CacheExceptionHandler getCacheExceptionHandler() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getCacheExceptionHandler();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void registerCacheLoader(CacheLoader arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.registerCacheLoader(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void unregisterCacheLoader(CacheLoader arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.unregisterCacheLoader(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public List getRegisteredCacheLoaders() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getRegisteredCacheLoaders();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void registerCacheWriter(CacheWriter arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.registerCacheWriter(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void unregisterCacheWriter() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.unregisterCacheWriter();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public CacheWriter getRegisteredCacheWriter() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getRegisteredCacheWriter();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Element getWithLoader(Object arg0, CacheLoader arg1, Object arg2) throws CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getWithLoader(arg0, arg1, arg2);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Map getAllWithLoader(Collection arg0, Object arg1) throws CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getAllWithLoader(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isDisabled() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isDisabled();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void setDisabled(boolean arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setDisabled(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Object getInternalContext() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getInternalContext();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void disableDynamicFeatures() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.disableDynamicFeatures();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public CacheWriterManager getWriterManager() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getWriterManager();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isClusterCoherent() throws TerracottaNotRunningException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isClusterCoherent();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isNodeCoherent() throws TerracottaNotRunningException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isNodeCoherent();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void setNodeCoherent(boolean arg0) throws UnsupportedOperationException, TerracottaNotRunningException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setNodeCoherent(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.waitUntilClusterCoherent();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void setTransactionManagerLookup(TransactionManagerLookup arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setTransactionManagerLookup(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Attribute getSearchAttribute(String arg0) throws CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getSearchAttribute(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Query createQuery() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.createQuery();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isSearchable() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isSearchable();
        } finally {
            t.setContextClassLoader(prev);
        }
    }
    /**
    * {@inheritDoc}
    */
    public void acquireReadLockOnKey(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.acquireReadLockOnKey(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void acquireWriteLockOnKey(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.acquireWriteLockOnKey(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean tryReadLockOnKey(Object arg0, long arg1) throws InterruptedException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.tryReadLockOnKey(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean tryWriteLockOnKey(Object arg0, long arg1) throws InterruptedException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.tryWriteLockOnKey(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void releaseReadLockOnKey(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.releaseReadLockOnKey(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void releaseWriteLockOnKey(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.releaseWriteLockOnKey(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isReadLockedByCurrentThread(Object arg0) throws UnsupportedOperationException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isReadLockedByCurrentThread(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isWriteLockedByCurrentThread(Object arg0) throws UnsupportedOperationException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isWriteLockedByCurrentThread(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isClusterBulkLoadEnabled() throws UnsupportedOperationException, TerracottaNotRunningException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isClusterBulkLoadEnabled();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isNodeBulkLoadEnabled() throws UnsupportedOperationException, TerracottaNotRunningException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isNodeBulkLoadEnabled();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void setNodeBulkLoadEnabled(boolean arg0) throws UnsupportedOperationException, TerracottaNotRunningException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setNodeBulkLoadEnabled(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void waitUntilClusterBulkLoadComplete() throws UnsupportedOperationException, TerracottaNotRunningException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.waitUntilClusterBulkLoadComplete();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void loadAll(Collection arg0, Object arg1) throws CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.loadAll(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void unpinAll() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.unpinAll();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean isPinned(Object arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.isPinned(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void setPinned(Object arg0, boolean arg1) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setPinned(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.toString();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Element get(Object arg0) throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.get(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Element get(Serializable arg0) throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.get(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void put(Element arg0) throws IllegalArgumentException, IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.put(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void put(Element arg0, boolean arg1) throws IllegalArgumentException, IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.put(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public Object clone() throws CloneNotSupportedException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.clone();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public String getName() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getName();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Element replace(Element arg0) throws NullPointerException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.replace(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean replace(Element arg0, Element arg1) throws NullPointerException, IllegalArgumentException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.replace(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void putAll(Collection arg0) throws IllegalArgumentException, IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.putAll(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean remove(Serializable arg0, boolean arg1) throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.remove(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean remove(Object arg0) throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.remove(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean remove(Serializable arg0) throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.remove(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean remove(Object arg0, boolean arg1) throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.remove(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void load(Object arg0) throws CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.load(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void setName(String arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.setName(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void flush() throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.flush();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public int getSize() throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getSize();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public boolean removeElement(Element arg0) throws NullPointerException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.removeElement(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void removeAll(boolean arg0) throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.removeAll(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void removeAll() throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.removeAll();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void removeAll(Collection arg0, boolean arg1) throws IllegalStateException, NullPointerException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.removeAll(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void removeAll(Collection arg0) throws IllegalStateException, NullPointerException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.removeAll(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Element putIfAbsent(Element arg0) throws NullPointerException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.putIfAbsent(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Element putIfAbsent(Element arg0, boolean arg1) throws NullPointerException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.putIfAbsent(arg0, arg1);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void addPropertyChangeListener(PropertyChangeListener arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.addPropertyChangeListener(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void removePropertyChangeListener(PropertyChangeListener arg0) {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.removePropertyChangeListener(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public void dispose() throws IllegalStateException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            this.cache.dispose();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public List getKeys() throws IllegalStateException, CacheException {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return Collections.unmodifiableList(new ClassLoaderAwareList(this.cache.getKeys()));
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
    * {@inheritDoc}
    */
    public Status getStatus() {
        // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return this.cache.getStatus();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
     * This class takes care of loading and unloading of classloader appropriately.
     * @author amaheshw
     *
     */
    private class ClassLoaderAwareList extends AbstractList {
        private final Collection delegate;

        public ClassLoaderAwareList(final Collection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object get(int index) {
            throw new UnsupportedOperationException("get(index) not supported for this List");
        }

        @Override
        public int size() {
            // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
            Thread t = Thread.currentThread();
            ClassLoader prev = t.getContextClassLoader();
            t.setContextClassLoader(classLoader);
            try {
                return this.delegate.size();
            } finally {
                t.setContextClassLoader(prev);
            }
        }

        @Override
        public Iterator iterator() {
            // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
            Thread t = Thread.currentThread();
            ClassLoader prev = t.getContextClassLoader();
            t.setContextClassLoader(classLoader);
            try {
                return new ClassLoaderAwareIterator(delegate.iterator());
            } finally {
                t.setContextClassLoader(prev);
            }
        }
    }

    /**
     * Iterator needed for ClassLoaderAwareList
     * @author amaheshw
     *
     */
    private class ClassLoaderAwareIterator implements Iterator {
        private final Iterator delegate;

        public ClassLoaderAwareIterator(final Iterator delegate) {
            this.delegate = delegate;
        }

        public boolean hasNext() {
            // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
            Thread t = Thread.currentThread();
            ClassLoader prev = t.getContextClassLoader();
            t.setContextClassLoader(classLoader);
            try {
                return delegate.hasNext();
            } finally {
                t.setContextClassLoader(prev);
            }
        }

        public Object next() {
            // THIS IS GENERATED CODE -- DO NOT HAND MODIFY!
            Thread t = Thread.currentThread();
            ClassLoader prev = t.getContextClassLoader();
            t.setContextClassLoader(classLoader);
            try {
                return delegate.next();
            } finally {
                t.setContextClassLoader(prev);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported for this Iterator");
        }
    }

}
