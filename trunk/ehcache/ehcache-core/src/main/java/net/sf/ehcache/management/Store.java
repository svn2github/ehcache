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

package net.sf.ehcache.management;

import java.io.Serializable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.StandardMBean;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.hibernate.management.impl.EhcacheHibernateMbeanNames;

/**
 * Wrapper class for store specific dynamic mbeans.
 *
 * @author Chris Dennis
 */
public final class Store implements Serializable, DynamicMBean {

    private static final long serialVersionUID = 3477287016924524437L;

    private final ObjectName objectName;
    private final DynamicMBean storeBean;

    private Store(Ehcache ehcache, Object storeBean) throws NotCompliantMBeanException {
        this.objectName = createObjectName(ehcache.getCacheManager().getName(), ehcache.getName());
        if (storeBean instanceof DynamicMBean) {
            this.storeBean = (DynamicMBean) storeBean;
        } else {
            this.storeBean = new StandardMBean(storeBean, null);
        }
    }

    /**
     * Get the optional store management bean for the given cache.
     */
    static Store getBean(Ehcache cache) throws NotCompliantMBeanException {
      if (cache instanceof net.sf.ehcache.Cache) {
        Object bean = ((net.sf.ehcache.Cache) cache).getStoreMBean();
        if (bean != null) {
          return new Store(cache, bean);
        }
      }
      return null;
    }

    /**
     * Creates an object name using the scheme "net.sf.ehcache:type=Store,CacheManager=<cacheManagerName>,name=<cacheName>"
     */
    static ObjectName createObjectName(String cacheManagerName, String cacheName) {
        try {
            return new ObjectName("net.sf.ehcache:type=Store,CacheManager=" + cacheManagerName + ",name="
                    + EhcacheHibernateMbeanNames.mbeanSafe(cacheName));
        } catch (MalformedObjectNameException e) {
            throw new CacheException(e);
        }
    }

    /**
     * @return the object name for this MBean
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    /**
     * {@inheritDoc}
     */
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return storeBean.getAttribute(attribute);
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        storeBean.setAttribute(attribute);
    }

    /**
     * {@inheritDoc}
     */
    public AttributeList getAttributes(String[] attributes) {
        return storeBean.getAttributes(attributes);
    }

    /**
     * {@inheritDoc}
     */
    public AttributeList setAttributes(AttributeList attributes) {
        return storeBean.setAttributes(attributes);
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return storeBean.invoke(actionName, params, signature);
    }

    /**
     * {@inheritDoc}
     */
    public MBeanInfo getMBeanInfo() {
        return storeBean.getMBeanInfo();
    }
}
