/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.services.impl;

import net.sf.ehcache.management.services.AccessorPrefix;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
* @author brandony
*/
abstract class ConstrainableEntityBuilderSupport<SAMPLER> {
  private Set<String> constraints;

  abstract Logger getLog();

  protected void addConstraints(Set<String> constraints) {
    if (constraints == null) throw new IllegalArgumentException("constraints == null");

    if (this.constraints == null) {
      this.constraints = constraints;
    } else {
      this.constraints.addAll(constraints);
    }
  }

  protected Set<String> getAttributeConstraints() {
    return constraints;
  }

  protected void buildAttributeMapByAttribute(Class<?> api,
                                   SAMPLER sampler,
                                   Map<String, Object> attributeMap,
                                   Collection<String> attributes,
                                   String nameAccessor) {
    for (String attribute : attributes) {
      Method method = null;
      for (AccessorPrefix prefix : AccessorPrefix.values()) {
        try {
          method = api.getMethod(prefix + attribute);
          break;
        } catch (NoSuchMethodException e) {
          //This is not the accessor you were looking for....move along
        }
      }

      if (method != null && !nameAccessor.equals(method.getName())) {
        addAttribute(sampler, attributeMap, attribute, method);
      }
    }
  }

  protected void buildAttributeMapByApi(Class<?> api,
                                   SAMPLER sampler,
                                   Map<String, Object> attributeMap,
                                   Collection<String> attributes,
                                   String nameAccessor) {
    for (Method method : api.getMethods()) {
      String name = method.getName();
      String trimmedName = AccessorPrefix.trimPrefix(name);
      if (!nameAccessor.equals(name) && AccessorPrefix.isAccessor(name) && (attributes == null || attributes.contains(
          trimmedName))) {
        addAttribute(sampler, attributeMap, trimmedName, method);
      }
    }
  }

  private void addAttribute(SAMPLER sampler,
                            Map<String, Object> attributeMap,
                            String attribute,
                            Method method) {
    try {
      attributeMap.put(attribute, method.invoke(sampler));
    } catch (IllegalAccessException e) {
      getLog().warn(String.format("Failed to invoke method %s while constructing entity due to access restriction.",
          method.getName()));
    } catch (InvocationTargetException e) {
      getLog().warn(String.format("Failed to invoke method %s while constructing entity. %s", method.getName(),
          e.getMessage()));
    }
  }
}
