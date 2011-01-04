/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.config.generator;

import net.sf.ehcache.config.generator.model.NodeAttribute;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.xsom.XSDAttributeValueFactory;
import net.sf.ehcache.config.generator.xsom.XSDAttributeValueType;

public class ValidDummyEhcacheConfigAttributesValueFactory implements XSDAttributeValueFactory {

    public String createValueForAttribute(NodeElement element, NodeAttribute attribute, XSDAttributeValueType xsdAttributeValueType) {
        if ("terracotta".equals(element.getName()) && "clustered".equals(attribute.getName())) {
            return "false";
        }
        if (("cache".equals(element.getName()) || "defaultCache".equals(element.getName()))
                && "transactionalMode".equals(attribute.getName())) {
            return "off";
        }
        // always generate with eternal=false
        if (("defaultCache".equals(element.getName()) || "cache".equals(element.getName())) && "eternal".equals(attribute.getName())) {
            return "false";
        }
        if ("maxMemoryOffHeap".equals(attribute.getName())) {
            return "1G";
        }
        if ("searchAttribute".equals(element.getName())) {
            if ("expression".equals(attribute.getName())) {
                return "value.toString()";
            } else if ("name".equals(attribute.getName())) {
                return "name";
            } else {
                return null;
            }
        }

        return xsdAttributeValueType.getRandomAllowedValue();
    }
}