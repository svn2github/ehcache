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

package net.sf.ehcache.statistics.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

/**
 * The ProxiedDynamicMBean. Supports truly dynamic mbeans in an easy
 * manner.
 *
 * @author cschanck
 */
public class ProxiedDynamicMBean implements DynamicMBean {

    private final TreeMap<String, AttributeProxy> map = new TreeMap<String, AttributeProxy>();
    private ArrayList<MBeanAttributeInfo> attributeInfoList;
    private MBeanInfo mbi;
    private final String beanName;
    private final String beanDescription;

    /**
     * Instantiates a new proxied dynamic m bean.
     *
     * @param beanName the bean name
     * @param beanDescription the bean description
     * @param attributeStandins the attribute standins
     */
    public ProxiedDynamicMBean(String beanName, String beanDescription, Collection<AttributeProxy> attributeStandins) {
        this.beanName = beanName;
        this.beanDescription = beanDescription;
        initialize(attributeStandins);
    }

    /**
     * Initialize.
     *
     * @param attributeStandins the attribute standins
     */
    public void initialize(Collection<AttributeProxy> attributeStandins) {
        for (AttributeProxy as : attributeStandins) {
            map.put(as.getName(), as);
        }
        attributeInfoList = new ArrayList<MBeanAttributeInfo>(map.size());

        for (Map.Entry<String, AttributeProxy> ent : map.entrySet()) {
            AttributeProxy standin = ent.getValue();
            MBeanAttributeInfo tmpInfo = new MBeanAttributeInfo(standin.getName(), standin.getTypeClass().getName(),
                    standin.getDescription(), standin.isRead(), standin.isWrite(), false);
            attributeInfoList.add(tmpInfo);
        }

        mbi = new MBeanInfo(getClass().getName(), beanDescription, attributeInfoList.toArray(new MBeanAttributeInfo[0]), null, null, null);
    }

    /**
     * Gets the bean name.
     *
     * @return the bean name
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * Get a specific attribtue
     */
    public Object getAttribute(String name) throws AttributeNotFoundException {

        AttributeProxy attr = map.get(name);
        if (attr != null && attr.isRead()) {
            return attr.get(name);
        }
        return "";

    }

    /**
     * Set an attribtue by name
     */
    public void setAttribute(Attribute attribute) throws InvalidAttributeValueException, MBeanException, AttributeNotFoundException {

        String name = attribute.getName();
        AttributeProxy attr = map.get(name);
        if (attr != null && attr.isWrite()) {
            attr.set(name, attribute.getValue());
        }
    }

    /**
     * Get a list of attributes.
     */
    public AttributeList getAttributes(String[] names) {

        AttributeList list = new AttributeList();
        for (String name : names) {
            AttributeProxy attr = map.get(name);
            if (attr != null && attr.isRead()) {
                Object value = attr.get(name);
                list.add(new Attribute(name, value));
            }

        }

        return list;

    }

    /**
     * Set an attribute list.
     */
    public AttributeList setAttributes(AttributeList list) {

        Attribute[] attrs = list.toArray(new Attribute[0]);

        AttributeList retlist = new AttributeList();

        for (Attribute attr : attrs) {
            String name = attr.getName();
            AttributeProxy a = map.get(name);
            if (a != null && a.isWrite()) {
                a.set(name, attr.getValue());
                retlist.add(attr);
            }
        }

        return retlist;

    }

    /**
     * Not implemented.
     */
    public Object invoke(String name, Object[] args, String[] sig) throws MBeanException, ReflectionException {
        return null;
    }

    /**
     * Accessor for mbean info
     */
    public MBeanInfo getMBeanInfo() {
        return mbi;
    }

}
