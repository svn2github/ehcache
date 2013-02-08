/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class BaseClusteredRegionFactoryTestServlet extends HttpServlet {

  public final void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/plain");
    PrintWriter out = response.getWriter();

    String server = request.getParameter("server");
    if ("server0".equals(server)) {
      try {
        doServer0(session, request.getParameterMap());
        out.println("OK");
      } catch (Exception e) {
        e.printStackTrace(out);
      }
    } else if ("server1".equals(server)) {
      try {
        doServer1(session, request.getParameterMap());
        out.println("OK");
      } catch (Exception e) {
        e.printStackTrace(out);
      }
    }
    out.flush();
  }
  
  protected abstract void doServer0(HttpSession session, Map<String, String[]> parameters) throws Exception;
  
  protected abstract void doServer1(HttpSession session, Map<String, String[]> parameters) throws Exception;
}
