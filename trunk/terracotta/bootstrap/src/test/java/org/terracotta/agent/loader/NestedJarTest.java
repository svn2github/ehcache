/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.agent.loader;

import net.sf.ehcache.CacheException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NestedJarTest extends TestCase {

  private static int count = 0;

  public void test() throws Exception {
    // dummywar.war contain WEB-INF/lib/containerjar.jar
    // containerjar.jar contains dummylib.jar
    // dummylib.jar contains DummyCallable.class
    /*
     * public class DummyCallable implements Callable<String> { 
     *  public String call() throws Exception { return "This is return value"; }
     * }
     */
    URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
    String innerJar = "jar:jar:" + url.toExternalForm() + "/dummywar.war!/WEB-INF/lib/containerjar.jar!/dummylib.jar";
    doTestInner(innerJar);

    innerJar = "jar:jar:jar:" + url.toExternalForm() + "/unsupportedNesting.jar!/dummywar.war!/WEB-INF/lib/containerjar.jar!/dummylib.jar";
    try {
      doTestInner(innerJar);
      fail("Should have thrown ClassNotFoundException");
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ClassNotFoundException);
    }
  }

  private void doTestInner(String innerJar) throws MalformedURLException, ClassNotFoundException, InstantiationException,
      IllegalAccessException, Exception, InterruptedException {
    JarManager jm = new JarManager(5000);
    URL innerJarUrl = new URL(innerJar);
    jm.getOrCreate(innerJar, innerJarUrl);
    Jar jar = jm.get(innerJar);
    Assert.assertNotNull("Jar must have been created", jar);
    URL callableUrl = newTcJarUrl(innerJarUrl, jm);
    URLClassLoader uc = new URLClassLoader(new URL[] { callableUrl });
    Class<Callable<String>> klass = (Class<Callable<String>>) uc.loadClass("org.dummy.DummyCallable");
    System.out.println("FOUND CLASS: " + klass.getName());

    Callable<String> callable = klass.newInstance();
    String rv = callable.call();
    Assert.assertEquals("This is return value", rv);
    System.out.println("Asserted value.. waiting for jar to deflate on idle (" + (jm.getIdleTime() / 1000) + " secs)");

    Thread.sleep(jm.getIdleTime() + 2000);
    Assert.assertNotNull("Jar cannot be null", jar);
    Assert.assertTrue("Jar must have been deflated", jar.isDeflated());
    System.out.println("Asserted jar deflated");
  }

  private static URL newTcJarUrl(final URL embedded, JarManager jm) {
    try {
      return new URL(Handler.TC_JAR_PROTOCOL, "", -1, Handler.TAG + embedded.toExternalForm() + Handler.TAG + "/", new Handler(jm));
    } catch (MalformedURLException e) {
      throw new CacheException(e);
    }
  }
}
