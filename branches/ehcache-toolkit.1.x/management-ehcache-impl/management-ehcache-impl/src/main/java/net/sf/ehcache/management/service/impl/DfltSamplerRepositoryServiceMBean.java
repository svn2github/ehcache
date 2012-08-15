/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

/**
 * @author Ludovic Orban
 */
public interface DfltSamplerRepositoryServiceMBean {

  /**
   * Invoke a method on the current object
   * @param methodName the name of the method to invoke
   * @param argsTypes the argument types
   * @param args the arguments
   * @return the result of the invocation in serialized form
   */
  byte[] invoke(String methodName, Class<?>[] argsTypes, Object[] args);

}
