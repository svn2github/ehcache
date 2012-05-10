/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.hibernate.management.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflective utilities for dealing with backward-incompatible change to statistics types in Hibernate 3.5.
 *
 * @author gkeim
 */
public class BeanUtils {
  /**
   * Return the named getter method on the bean or null if not found.
   * 
   * @param bean
   * @param propertyName
   * @return the named getter method
   */
  private static Method getMethod(Object bean, String propertyName) {
    StringBuilder sb = new StringBuilder("get").append(Character.toUpperCase(propertyName.charAt(0)));
    if (propertyName.length() > 1) {
      sb.append(propertyName.substring(1));
    }
    String getterName = sb.toString();
    for (Method m : bean.getClass().getMethods()) {
      if (getterName.equals(m.getName()) && m.getParameterTypes().length == 0) {
        return m;
      }
    }
    return null;
  }

  /**
   * Return the named field on the bean or null if not found.
   * 
   * @param bean
   * @param propertyName
   * @return the named field
   */
  private static Field getField(Object bean, String propertyName) {
    for (Field f : bean.getClass().getDeclaredFields()) {
      if (propertyName.equals(f.getName())) {
        return f;
      }
    }
    return null;
  }
  
  private static void validateArgs(Object bean, String propertyName) {
    if (bean == null) {
      throw new IllegalArgumentException("bean is null");
    }
    if (propertyName == null) {
      throw new IllegalArgumentException("propertyName is null");
    }
    if (propertyName.trim().length() == 0) {
      throw new IllegalArgumentException("propertyName is empty");
    }
  }
  
  /**
   * Retrieve a named bean property value.
   * 
   * @param bean bean
   * @param propertyName
   * @return the property value
   */
  public static Object getBeanProperty(Object bean, String propertyName) {
    validateArgs(bean, propertyName);
    
    // try getters first
    Method getter = getMethod(bean, propertyName);
    if (getter != null) {
      try {
        return getter.invoke(bean);
      } catch (Exception e) {
        /**/
      }
    }
    
    // then try fields
    Field field = getField(bean, propertyName);
    if (field != null) {
      try {
        field.setAccessible(true);
        return field.get(bean);
      } catch (Exception e) {
        /**/
      }
    }
        
    return null;
  }
    
  /**
   * Retrieve a Long bean property value.
   * 
   * @param bean bean
   * @param propertyName
   * @return long value
   * @throws NoSuchFieldException
   */
  public static long getLongBeanProperty(final Object bean, final String propertyName) throws NoSuchFieldException {
    validateArgs(bean, propertyName);
    Object o = getBeanProperty(bean, propertyName);
    if (o == null) {
      throw new NoSuchFieldException(propertyName);
    } else if (!(o instanceof Number)) {
      throw new IllegalArgumentException(propertyName + " not an Number");
    }
    return ((Number)o).longValue();
  }
}
