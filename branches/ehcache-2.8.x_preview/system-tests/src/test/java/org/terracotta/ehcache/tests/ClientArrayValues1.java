package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

public class ClientArrayValues1 extends ClientBase {

  public ClientArrayValues1(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ClientArrayValues1(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    cache.put(new Element("key", new String[] { "a", "b", "c" }));
//    } else {
//      Element elem = cache.get("key");
//      assertNotNull()
//      String[] value = elem.getValue();
//    }
  }
}
