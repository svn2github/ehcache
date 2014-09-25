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

package net.sf.ehcache.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
     * Creates a new class instance with the given loader. Logs errors along the way.
     *
     * @param loader the classloader to load the class
     * @param className a fully qualified class name
     * @return the newly created instance
     * @throws CacheException if instance cannot be created due to a missing class or exception
     */
    public static Object createNewInstance(ClassLoader loader, String className) throws CacheException {
        return createNewInstance(loader, className, new Class[0], new Object[0]);
    }
   
    /**
     * Creates a new class instance and passes args to the constructor call. Logs errors along the way.
     *
     * @param loader the classloader to load the class
     * @param className a fully qualified class name
     * @param argTypes Types for constructor argument parameters
     * @param args Values for constructor argument parameters 
     * @return the newly created instance
     * @throws CacheException if instance cannot be created due to a missing class or exception
     */
    public static Object createNewInstance(ClassLoader loader, String className, Class[] argTypes, Object[] args) throws CacheException {
        Class clazz;
        Object newInstance;
        try {
            clazz = loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        }

        try {
            Constructor constructor = clazz.getConstructor(argTypes);
            newInstance = constructor.newInstance(args);
        } catch (IllegalAccessException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new CacheException("Unable to load class " + className +
                    ". Initial cause was " + e.getCause().getMessage(), e.getCause());
        }
        return newInstance;
    }



}
