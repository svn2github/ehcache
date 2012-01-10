/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

public class ThreadContextAwareClassLoader extends ClassLoader {

  public ThreadContextAwareClassLoader(ClassLoader parent) {
    super(parent);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    // Check whether it's already loaded
    Class loadedClass = findLoadedClass(name);
    if (loadedClass != null) { return loadedClass; }

    // Try to load from thread context classloader, if it exists
    try {
      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      return Class.forName(name, false, tccl);
    } catch (ClassNotFoundException e) {
      // Swallow exception - does not exist in tccl
    }

    // If not found locally, use normal parent delegation
    return super.loadClass(name);
  }

}
