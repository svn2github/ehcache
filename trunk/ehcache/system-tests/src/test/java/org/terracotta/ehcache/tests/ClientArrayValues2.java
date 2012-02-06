package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class ClientArrayValues2 extends ClientBase {

  public ClientArrayValues2(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ClientArrayValues2(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
      Element elem = cache.get("key");
      if(elem == null) {
        throw new AssertionError("No element!");
      }
      String[] value = (String[])elem.getValue();
      if(value.length != 3 || !value[0].equals("a") || !value[1].equals("b") || !value[2].equals("c")) {
        throw new AssertionError("Didn't get String[] { \"a\", \"b\", \"c\"");
      }
      
  }
}
