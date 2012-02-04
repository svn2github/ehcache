package org.terracotta.ehcache.tests.loader;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.terracotta.tests.base.AbstractClientBase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoaderClient extends AbstractClientBase {
  private static final int INDEX_WHERE_TO_SNEAK_DOT_DOT = 2;
  private final String     specialClassPath;

  public LoaderClient(String[] args) {
    super(args);
    this.specialClassPath = args[1];
  }

  public static void main(String[] args) throws Exception {
    new LoaderClient(args).run();
  }

  @Override
  protected void doTest() throws Throwable {
    System.setProperty("terracottaUrl", getTerracottaUrl());

    Map<String, byte[]> extra = new HashMap<String, byte[]>();
    extra.put(Asserter.class.getName(), getClassBytes(Asserter.class));

    String[] specialCpEntries = specialClassPath.split(File.pathSeparator);

    URL[] urls = new URL[specialCpEntries.length];
    for (int i = 0; i < urls.length; i++) {
      specialCpEntries[i] = "file:" + specialCpEntries[i];
      if (!specialCpEntries[i].endsWith(".jar") && !specialCpEntries[i].endsWith("/")) specialCpEntries[i] = specialCpEntries[i]
                                                                                                             + "/";

      if (specialCpEntries[i].endsWith(".jar")) urls[i] = sneakDotDotInUrl(new URL(specialCpEntries[i]));
      else urls[i] = new URL(specialCpEntries[i]);

    }

    Loader loader = new Loader(urls, null, extra);
    Runnable r = (Runnable) loader.loadClass(Asserter.class.getName()).newInstance();
    r.run();

  }

  private static URL sneakDotDotInUrl(URL url) throws MalformedURLException {
    String path = url.getPath();
    String protocol = url.getProtocol();
    List<String> pathEntries = new ArrayList<String>(Arrays.asList(path.split("\\/")));

    if (pathEntries.size() < INDEX_WHERE_TO_SNEAK_DOT_DOT + 1) { throw new AssertionError(
                                                                                          "path to url ("
                                                                                              + url
                                                                                              + ") is too short - it must contain at least "
                                                                                              + (INDEX_WHERE_TO_SNEAK_DOT_DOT + 1)
                                                                                              + " path separators"); }

    String entry = pathEntries.get(INDEX_WHERE_TO_SNEAK_DOT_DOT);
    pathEntries.add(INDEX_WHERE_TO_SNEAK_DOT_DOT + 1, "..");
    pathEntries.add(INDEX_WHERE_TO_SNEAK_DOT_DOT + 2, entry);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pathEntries.size(); i++) {
      String s = pathEntries.get(i);
      sb.append(s);
      if (i < pathEntries.size() - 1) sb.append("/");
    }
    path = sb.toString();

    return new URL(protocol + ":" + path);
  }

  private static byte[] getClassBytes(Class<?> clazz) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    InputStream in = LoaderClient.class.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/')
                                                                                 .concat(".class"));

    int b;
    while ((b = in.read()) >= 0) {
      baos.write(b);
    }

    return baos.toByteArray();
  }

  public static class Loader extends URLClassLoader {

    private final Map<String, byte[]> extra;

    public Loader(URL[] urls, ClassLoader parent, Map<String, byte[]> extra) {
      super(urls, parent);
      this.extra = extra;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      byte[] b = extra.remove(name);
      if (b != null) { return defineClass(name, b, 0, b.length); }

      return super.findClass(name);
    }

  }

  public static class Asserter implements Runnable {

    public void run() {
      CacheManager cacheManager = new CacheManager(Asserter.class.getResourceAsStream("/ehcache-config.xml"));

      Cache cache = cacheManager.getCache("test");
      cache.put(new Element("1", "one"));

      if (!cache.get("1").getValue().equals("one")) throw new AssertionError();

      cacheManager.shutdown();
    }
  }

}
