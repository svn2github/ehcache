/**
 *  Copyright 2003-2007 Greg Luck
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

package net.sf.ehcache.util;

import net.sf.ehcache.CacheException;

/**
 * Keeps all classloading in ehcache consistent.
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class ClassLoaderUtil {

    /**
     * Utility class.
     */
    private ClassLoaderUtil() {
        //noop
    }

    /**
     * Gets the <code>ClassLoader</code> that all classes in ehcache, and extensions, should
     * use for classloading. All ClassLoading in ehcache should use this one. This is the only
     * thing that seems to work for all of the class loading situations found in the wild.
     * @return the thread context class loader.
     */
    public static ClassLoader getStandardClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Gets a fallback <code>ClassLoader</code> that all classes in ehcache, and extensions,
     * should use for classloading. This is used if the context class loader does not work.
     * @return the <code>ClassLoaderUtil.class.getClassLoader();</code>
     */
    public static ClassLoader getFallbackClassLoader() {
        return ClassLoaderUtil.class.getClassLoader();
    }

    /**
     * Creates a new class instance. Logs errors along the way. Classes are loaded using the
     * ehcache standard classloader.
     *
     * @param className a fully qualified class name
     * @return null if the instance cannot be loaded
     */
    public static Object createNewInstance(String className) throws CacheException {
        Class clazz;
        Object newInstance;
        try {
            clazz = Class.forName(className, true, getStandardClassLoader());
        } catch (ClassNotFoundException e) {
            //try fallback
            try {
                clazz = Class.forName(className, true, getFallbackClassLoader());
            } catch (ClassNotFoundException ex) {
                throw new CacheException("Unable to load class " + className +
                        ". Initial cause was " + e.getMessage(), e);
            }
        }

        try {
            newInstance = clazz.newInstance();
        } catch (IllegalAccessException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        }
        return newInstance;
    }


}
