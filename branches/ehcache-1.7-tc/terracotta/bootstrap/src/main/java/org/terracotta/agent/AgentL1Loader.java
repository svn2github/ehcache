/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.agent;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

class AgentL1Loader extends URLClassLoader {

  private final Map<String, byte[]> extraClassDefs;

  public AgentL1Loader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs) {
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
