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

import net.sf.ehcache.Element;
import net.sf.ehcache.terracotta.InternalEhcache;

/**
 * Extension of Class Loader Aware cache to accommodate the removeAndReturnElement method.
 *
 * @author dhruv
 */
public class InternalClassLoaderAwareCache extends ClassLoaderAwareCache implements InternalEhcache {

    /**
     * Constructor
     *
     * @param cache wrapped cache
     * @param classLoader loader to set Thread context loader to for duration of cache operation
     */
    public InternalClassLoaderAwareCache(InternalEhcache cache, ClassLoader classLoader) {

        super(cache, classLoader);
    }

    /**
     * {@inheritDoc}
     */
    public Element removeAndReturnElement(Object arg0) throws IllegalStateException {
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            return ((InternalEhcache) this.cache).removeAndReturnElement(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void recalculateSize(Object arg0) {
        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        t.setContextClassLoader(this.classLoader);
        try {
            ((InternalEhcache) this.cache).recalculateSize(arg0);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

}
