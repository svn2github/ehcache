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

package net.sf.ehcache.config.generator;

import net.sf.ehcache.config.generator.model.NodeAttribute;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.xsom.XSDAttributeValueFactory;
import net.sf.ehcache.config.generator.xsom.XSDAttributeValueType;

public class StandaloneEntryBasedConfigAttributesValueFactory implements XSDAttributeValueFactory {

    public String createValueForAttribute(NodeElement element, NodeAttribute attribute, XSDAttributeValueType xsdAttributeValueType) {
        if ("terracotta".equals(element.getName())) {
            if("coherent".equals(attribute.getName())) {
                // returning null will skip this attribute
                // can skip "coherent" attribute as its deprecated by consistency attribute
                return null;
            }
            if("clustered".equals(attribute.getName())) {
                return "false";
            }
        }
        if ("pinning".equals(element.getName())) {
            if ("storage".equals(attribute.getName())) {
                return "inMemory";
            }
        }
        if ("sizeOfPolicy".equals(element.getName())) {
            if ("maxDepth".equals(attribute.getName())) {
                return "100";
            } else if ("maxDepthExceededBehavior".equals(attribute.getName())) {
                return "continue";
            }
        }
        // always generate with eternal=false
        if ("defaultCache".equals(element.getName()) || "cache".equals(element.getName())) {
            if("eternal".equals(attribute.getName())) {
                return "false";
            }
            if("transactionalMode".equals(attribute.getName())) {
                return "off";
            }
            // these are deprecated
            if ("maxElementsInMemory".equals(attribute.getName()) ||
                    "maxMemoryOffHeap".equals(attribute.getName()) ||
                    "diskPersistent".equals(attribute.getName()) ||
                    "overflowToDisk".equals(attribute.getName())) {
                return null;
            }
            if ("maxElementsOnDisk".equals(attribute.getName()) ||
                    "maxBytesLocalHeap".equals(attribute.getName()) ||
                    "maxBytesLocalOffHeap".equals(attribute.getName()) ||
                    "maxBytesLocalDisk".equals(attribute.getName())) {
                return null;
            }
            if("maxEntriesLocalHeap".equals(attribute.getName()) ||
                    "maxEntriesLocalOffHeap".equals(attribute.getName()) ||
                    "maxEntriesLocalDisk".equals(attribute.getName())) {
                return "10000";
            }
        }
        if ("ehcache".equals(element.getName())) {
            if("maxBytesLocalHeap".equals(attribute.getName()) ||
                    "maxBytesLocalOffHeap".equals(attribute.getName()) ||
                    "maxBytesLocalDisk".equals(attribute.getName())) {
                return null;
            }
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
