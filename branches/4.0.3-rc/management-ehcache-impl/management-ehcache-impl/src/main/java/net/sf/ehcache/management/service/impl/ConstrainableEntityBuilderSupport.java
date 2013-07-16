/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.service.AccessorPrefix;
import net.sf.ehcache.util.counter.Counter;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
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
    Set<String> excludedNames = getExcludedAttributeNames(sampler);

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
        if (excludedNames.contains(attribute)) {
          attributeMap.put(attribute, 0);
          continue;
        }

        addAttribute(sampler, attributeMap, attribute, method);
      }
    }
  }

  protected void buildAttributeMapByApi(Class<?> api,
                                   SAMPLER sampler,
                                   Map<String, Object> attributeMap,
                                   Collection<String> attributes,
                                   String nameAccessor) {
    Set<String> excludedNames = getExcludedAttributeNames(sampler);

    for (Method method : api.getMethods()) {
      String name = method.getName();
      String trimmedName = AccessorPrefix.trimPrefix(name);
      if (!nameAccessor.equals(name) && AccessorPrefix.isAccessor(name) && (attributes == null || attributes.contains(
          trimmedName))) {

        if (excludedNames.contains(trimmedName)) {
          attributeMap.put(trimmedName, 0);
          continue;
        }

        addAttribute(sampler, attributeMap, trimmedName, method);
      }
    }
  }

  protected abstract Set<String> getExcludedAttributeNames(SAMPLER sampler);

  private void addAttribute(SAMPLER sampler,
                            Map<String, Object> attributeMap,
                            String attribute,
                            Method method) {
    Object value = null;
    try {
      value = method.invoke(sampler);

      // stats reflection "helper" code
      if (value instanceof SampledCounter) {
        value = ((SampledCounter)value).getMostRecentSample().getCounterValue();
      } else if (value instanceof Counter) {
        value = ((Counter)value).getValue();
      }

    } catch (RuntimeException e) {
      getLog().warn(String.format("Failed to invoke method %s while constructing entity. %s", method.getName(),
          e.getMessage()));
    } catch (IllegalAccessException e) {
      getLog().warn(String.format("Failed to invoke method %s while constructing entity due to access restriction.",
          method.getName()));
    } catch (InvocationTargetException e) {
      getLog().warn(String.format("Failed to invoke method %s while constructing entity. %s", method.getName(),
          e.getMessage()));
    } finally {
      attributeMap.put(attribute, value);
    }
  }
}
