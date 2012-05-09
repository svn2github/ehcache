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

package net.sf.ehcache.search.attribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.ehcache.Element;

/**
 * Extracts a search attribute determining the value as a javabean property on either
 * the key or the value. If the property exists on both the key and the value an exception is thrown
 *
 * @author teck
 */
/**
 * @author teck
 *
 */
public class JavaBeanAttributeExtractor implements AttributeExtractor {

    private static final Object NO_VALUE = new Object();

    private transient volatile MethodRef lastKeyMethod;
    private transient volatile MethodRef lastValueMethod;

    private final String isMethodName;
    private final String getMethodName;
    private final String beanProperty;

    /**
     * Constructor
     *
     * @param beanProperty the bean property name to extract
     */
    public JavaBeanAttributeExtractor(String beanProperty) {
        if (beanProperty == null) {
            throw new NullPointerException();
        }

        if (beanProperty.length() == 0) {
            throw new IllegalArgumentException("bean property empty");
        }

        this.beanProperty = beanProperty;

        String upperFirstProp = "" + Character.toUpperCase(beanProperty.charAt(0));
        if (beanProperty.length() > 1) {
            upperFirstProp += beanProperty.substring(1);
        }

        isMethodName = "is" + upperFirstProp;
        getMethodName = "get" + upperFirstProp;
    }

    /**
     * {@inheritDoc}
     */
    public Object attributeFor(Element element, String attributeName) throws AttributeExtractorException {
        Object attribute = NO_VALUE;

        final Object key = element.getObjectKey();

        if (key != null) {
            MethodRef keyMethod = lastKeyMethod;
            if (keyMethod == null || keyMethod.targetClass != key.getClass()) {
                keyMethod = findMethod(key);
                lastKeyMethod = keyMethod;
            }
            if (keyMethod.method != null) {
                attribute = getValue(keyMethod.method, key);
            }
        }

        final Object value = element.getObjectValue();

        if (value != null) {
            MethodRef valueMethod = lastValueMethod;
            if (valueMethod == null || valueMethod.targetClass != value.getClass()) {
                valueMethod = findMethod(value);
                lastValueMethod = valueMethod;
            }

            if (valueMethod.method != null) {
                if (attribute != NO_VALUE) {
                    throw new AttributeExtractorException("Bean property [" + beanProperty + "] present on both key and value");
                }

                return getValue(valueMethod.method, value);
            }
        }

        if (attribute != NO_VALUE) {
            return attribute;
        }

        throw new AttributeExtractorException("Bean property [" + beanProperty + "] not present on either key or value");
    }

    private MethodRef findMethod(Object obj) {
        final Class target = obj.getClass();

        try {
            return new MethodRef(target, target.getMethod(getMethodName));
        } catch (SecurityException e) {
            throw new AttributeExtractorException(e);
        } catch (NoSuchMethodException e) {
            // keep looking
        }

        try {
            Method m = target.getMethod(isMethodName);
            if (m.getReturnType().equals(Boolean.class) || m.getReturnType().equals(Boolean.TYPE)) {
                return new MethodRef(target, m);
            }
        } catch (SecurityException e) {
            throw new AttributeExtractorException(e);
        } catch (NoSuchMethodException e) {
            //
        }

        // no applicable method available
        return new MethodRef(target, null);
    }

    private Object getValue(Method method, Object key) {
        try {
            return method.invoke(key);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t = t.getCause();
            }

            if (t instanceof Error) {
                throw ((Error) t);
            }

            throw new AttributeExtractorException("Error getting bean property [" + beanProperty + "] on instance of "
                    + key.getClass().getName(), t);
        }
    }

    /**
     * A cached method lookup. Method is null to indicate the method is not present/accessible
     */
    private static class MethodRef {
        private final Class targetClass;
        private final Method method;

        MethodRef(Class target, Method method) {
            this.targetClass = target;
            this.method = method;
        }
    }

}
