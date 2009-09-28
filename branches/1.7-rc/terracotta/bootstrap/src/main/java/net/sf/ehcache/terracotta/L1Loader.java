/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.terracotta;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class L1Loader extends URLClassLoader {

  private final Map<String, byte[]> extraClassDefs;

  public L1Loader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs) {
    super(urls, parent);
    this.extraClassDefs = new HashMap<String, byte[]>(extraClassDefs);
  }

  @Override
  protected Class<?> findClass(final String name) throws ClassNotFoundException {
    byte[] classBytes = this.extraClassDefs.remove(name);
    if (classBytes != null) { return defineClass(name, classBytes, 0, classBytes.length); }

    return super.findClass(name);
  }

}
