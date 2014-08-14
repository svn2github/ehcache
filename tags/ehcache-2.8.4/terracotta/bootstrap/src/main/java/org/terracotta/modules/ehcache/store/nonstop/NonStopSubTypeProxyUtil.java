/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.nonstop;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.nonstop.NonStopException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Utility to create a Proxy for Subtypes to catch NonStopException and translate that to NonStopCacheException.
 */
public class NonStopSubTypeProxyUtil {

  private static Set<Class> SUPPORTED_SUB_TYPES = new HashSet<Class>();

  static {
    SUPPORTED_SUB_TYPES.add(Iterator.class);
    SUPPORTED_SUB_TYPES.add(ListIterator.class);
    SUPPORTED_SUB_TYPES.add(Collection.class);
    SUPPORTED_SUB_TYPES.add(Set.class);
    SUPPORTED_SUB_TYPES.add(List.class);
    SUPPORTED_SUB_TYPES.add(Map.class);
  }

  static boolean isNonStopSubtype(Class klazz) {
    return SUPPORTED_SUB_TYPES.contains(klazz);
  }

  public static <E> E newNonStopSubTypeProxy(Class<E> klazz, E delegate) {
    InvocationHandler handler = new NonStopSubTypeInvocationHandler(delegate);

    E proxy = (E) Proxy.newProxyInstance(klazz.getClassLoader(), new Class[] { klazz }, handler);
    return proxy;
  }

  private static class NonStopSubTypeInvocationHandler implements InvocationHandler {
    private final Object delegate;

    public NonStopSubTypeInvocationHandler(Object delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      try {
        Object rv = invokeMethod(method, args, delegate);
        return createNonStopSubtypeIfNecessary(rv, method.getReturnType());
      } catch (NonStopException e) {
        throw new NonStopCacheException(e);
      }
    }

    protected Object createNonStopSubtypeIfNecessary(Object returnValue, Class klazzParam) {
      if (isNonStopSubtype(klazzParam)) {
        return newNonStopSubTypeProxy(klazzParam, returnValue);
      } else {
        return returnValue;
      }
    }

    private Object invokeMethod(Method method, Object[] args, Object object) throws Throwable {
      try {
        return method.invoke(object, args);
      } catch (InvocationTargetException t) {
        throw t.getTargetException();
      } catch (IllegalArgumentException e) {
        throw new ToolkitRuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new ToolkitRuntimeException(e);
      }
    }
  }
}
