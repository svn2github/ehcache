package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;

import org.terracotta.api.ClusteringToolkit;

import com.otherclassloader.Client;
import com.otherclassloader.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

public class OtherClassLoaderEventClient2 extends ClientBase {

  public OtherClassLoaderEventClient2(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new OtherClassLoaderEventClient2(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {

    String config = cache.getCacheManager().getActiveConfigurationText();

    // This loader simulates ehcache in an isolated loader (similiar to an osgi bundle)
    EhcacheLoader ehcacheLoader = new EhcacheLoader();

    // make sure the "app" types not visible to loader where ehcache is running
    try {
      ehcacheLoader.loadClass(Value.class.getName());
      throw new AssertionError();
    } catch (ClassNotFoundException cnfe) {
      // expected
    }

    // The "app" loader is like an applicatoin bundle that imports ehcache
    AppLoader appLoader = new AppLoader(ehcacheLoader);
    Thread.currentThread().setContextClassLoader(appLoader);

    Class clientClass = appLoader.loadClass(Client.class.getName());
    final CyclicBarrier localBarrier = new CyclicBarrier(2);
    final Runnable client = (Runnable) clientClass.getConstructor(String.class, CyclicBarrier.class)
        .newInstance(config, localBarrier);
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          client.run();
        } catch (Throwable t) {
          t.printStackTrace();
          error.set(t);
        }
      }
    };
    thread.start();

    localBarrier.await();
    getBarrierForAllClients().await();

    thread.join();
    if (error.get() != null) { throw error.get(); }
  }

  static class EhcacheLoader extends URLClassLoader {
    EhcacheLoader() {
      super(((URLClassLoader) (ClassLoader.getSystemClassLoader())).getURLs(), null);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      // hide "app" classes from loader used to load ehcache
      if (name.startsWith("com.otherclassloader.")) { throw new ClassNotFoundException(name); }

      return super.findClass(name);
    }

  }

  static class AppLoader extends ClassLoader {

    AppLoader(EhcacheLoader ehcacheLoader) {
      super(ehcacheLoader);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      if (name.startsWith("com.otherclassloader.")) {
        InputStream in = ClassLoader.getSystemClassLoader()
            .getResourceAsStream(name.replace('.', '/').concat(".class"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
          while (in.available() > 0) {
            baos.write(in.read());
          }
          byte[] data = baos.toByteArray();
          return defineClass(name, data, 0, data.length);
        } catch (IOException ioe) {
          throw new ClassNotFoundException(name, ioe);
        }
      }
      throw new ClassNotFoundException(name);
    }
  }

}
