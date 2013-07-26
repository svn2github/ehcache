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
   * Invoke a method on the current object.
   *
   * @param ticket the security ticket, can be null if there is no security context to pass on
   * @param token the security token, can be null if there is no security context to pass on
   * @param iaCallbackUrl the security IA callback URL
   * @param methodName the name of the method to invoke
   * @param argsTypes the argument types
   * @param args the arguments
   * @return the result of the invocation in serialized form
   */
  byte[] invoke(String ticket, String token, String iaCallbackUrl, String methodName, Class<?>[] argsTypes, Object[] args);

  /**
   * Get the implementation version of this MBean.
   *
   * @return the version.
   */
  String getVersion();

  /**
   * Get the agency of this MBean.
   *
   * @return the version.
   */
  String getAgency();

}
