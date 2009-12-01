/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.junit.Assert;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BasicTestServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/plain");
    PrintWriter out = resp.getWriter();

    String cmd = req.getParameter("cmd");
    debug("Doing command: " + cmd);
    if ("insert".equals(cmd)) {
      doInsert();
    } else if ("query".equals(cmd)) {
      doQuery();
    } else {
      out.println("Unknown command: " + cmd);
      return;
    }
    out.println("OK");
  }

  private void doInsert() {
    CacheManager mgr = CacheManager.getInstance();
    Cache cache = mgr.getCache("test");
    Assert.assertNotNull("Cache with name 'test' cannot be null as its present in config", cache);
    Element el = new Element("key", "value");
    cache.put(el);
    debug("Added element - " + el);
  }

  private void doQuery() {
    CacheManager mgr = CacheManager.getInstance();
    Cache cache = mgr.getCache("test");
    Assert.assertNotNull("Cache with name 'test' cannot be null as its present in config", cache);
    Element el = cache.get("key");
    debug("Got element - " + el);
    Assert.assertNotNull("Element cannot be null", el);
    Object val = el.getObjectValue();
    Assert.assertEquals("value", val);
  }

  private void debug(String string) {
    System.out.println(string);

  }

}
