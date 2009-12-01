/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.terracotta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.net.URLClassLoader;

public class AppLevelTIMLoader extends URLClassLoader {

  private final ClassLoader             appLoader;
  private volatile ClassFileTransformer transformer;

  public AppLevelTIMLoader(URL[] urls, ClassLoader parent, ClassLoader appLoader) {
    super(urls, parent);
    this.appLoader = appLoader;
  }

  void setTransformer(ClassFileTransformer transformer) {
    this.transformer = transformer;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    Class<?> rv = findLoadedClass(name);
    if (rv != null) { return rv; }

    // Delegate to parent first (boot jar loader)
    try {
      return getParent().loadClass(name);
    } catch (ClassNotFoundException cnfe) {
      //
    }

    // Next search local paths
    String resName = name.replace('.', '/').concat(".class");
    URL resource = getResource(resName);
    if (resource != null) {
      byte[] clazzBytes;
      try {
        clazzBytes = readResource(resource);
      } catch (IOException ioe) {
        throw new ClassNotFoundException(name, ioe);
      }
      try {
        clazzBytes = transformer.transform(this, name, null, null, clazzBytes);
      } catch (IllegalClassFormatException e) {
        throw new AssertionError(e);
      }

      return defineClass(name, clazzBytes, 0, clazzBytes.length);
    }

    // last path is to delegate to the app loader
    return appLoader.loadClass(name);
  }

  private byte[] readResource(URL resource) throws IOException {
    InputStream in = null;

    try {
      in = resource.openStream();

      byte[] buf = new byte[4096];
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int read;
      while ((read = in.read(buf)) > 0) {
        baos.write(buf, 0, read);
      }

      return baos.toByteArray();
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }

  }

}
