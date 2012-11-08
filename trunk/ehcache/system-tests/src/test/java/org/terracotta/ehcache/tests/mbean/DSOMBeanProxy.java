/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.mbean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

/**
 * @author Abhishek Sanoujam
 */
public class DSOMBeanProxy extends MBeanServerInvocationHandler {

  public DSOMBeanProxy(MBeanServerConnection mbs, ObjectName objectName) {
    super(mbs, objectName);
  }

  public static DSOMBean newL2ControlMBeanProxy(MBeanServerConnection connection, ObjectName objectName) {
    final InvocationHandler handler = new DSOMBeanProxy(connection, objectName);
    final Class[] interfaces = { DSOMBean.class };
    Object proxy = Proxy.newProxyInstance(DSOMBean.class.getClassLoader(), interfaces, handler);
    return DSOMBean.class.cast(proxy);
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return super.invoke(proxy, method, args);
  }

}
