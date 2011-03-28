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

package net.sf.ehcache.config.generator.xsom;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.SimpleNodeElement;

import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.XmlString;
import com.sun.xml.xsom.parser.XSOMParser;

public class XSOMHelper {
    private XSDAttributeValueFactory attributeValueFactory;

    public XSOMHelper(XSDAttributeValueFactory attributeValueFactory) {
        this.attributeValueFactory = attributeValueFactory;
    }

    public XSDAttributeValueFactory getAttributeValueFactory() {
        return attributeValueFactory;
    }

    public void setAttributeValueFactory(XSDAttributeValueFactory attributeValueFactory) {
        this.attributeValueFactory = attributeValueFactory;
    }

    public NodeElement createRootElement(InputStream xsdInputStream, String rootName) throws Exception {
        XSOMParser parser = new XSOMParser();
        parser.parse(xsdInputStream);
        XSSchemaSet schemaSet = parser.getResult();

        Iterator<XSSchema> schemaIterator = schemaSet.iterateSchema();
        XSElementDecl rootElementDecl = null;
        while (schemaIterator.hasNext()) {
            XSSchema schema = schemaIterator.next();
            rootElementDecl = schema.getElementDecl(rootName);
            if (rootElementDecl != null)
                break;
        }
        if (rootElementDecl == null) {
            throw new Exception("Root Element not found - " + rootName);
        }

        return createElement(null, rootElementDecl);
    }

    private XSDElement createElement(SimpleNodeElement parent, XSElementDecl elementDecl) throws Exception {
        XSDElement element = new XSDElement(parent, elementDecl.getName());
        List<XSAttributeUse> attrs = getXSAttributeUses(elementDecl);
        for (XSAttributeUse attrUse : attrs) {
            XSDAttribute attribute = createXSDAttribute(element, attrUse);
            if (attribute != null) {
                element.addAttribute(attribute);
            }
        }
        for (XSElementDeclWrapper childXSElementDeclWrapper : getChildElements(elementDecl)) {
            XSDElement childElement = createElement(element, childXSElementDeclWrapper.getXsElementDecl());
            childElement.setMinOccurs(childXSElementDeclWrapper.getMinOccurs());
            childElement.setMaxOccurs(childXSElementDeclWrapper.getMaxOccurs());
            element.addChildElement(childElement);
        }
        return element;
    }

    private List<XSAttributeUse> getXSAttributeUses(XSElementDecl element) {
        if (!element.getType().isComplexType()) {
            return Collections.EMPTY_LIST;
        }
        return new ArrayList<XSAttributeUse>(element.getType().asComplexType().getAttributeUses());
    }

    private XSDAttribute createXSDAttribute(NodeElement element, XSAttributeUse attributeUse) throws Exception {
        XSAttributeDecl attributeDecl = attributeUse.getDecl();
        XSDAttribute attribute = new XSDAttribute(attributeDecl.getName());
        XSDAttributeValueType attributeValueType = XSDAttributeValueTypeFactory.createType(attributeDecl);
        attribute.setAttributeValueType(attributeValueType);

        String attributeValue = attributeValueFactory.createValueForAttribute(element, attribute, attributeValueType);
        if (attributeValue == null) {
            return null;
        }
        attribute.setValue(attributeValue);

        attribute.setOptional(!attributeUse.isRequired());
        XmlString defaultValue = attributeUse.getDefaultValue();
        if (defaultValue != null) {
            attribute.setDefaultValue(defaultValue.value);
        }
        return attribute;
    }

    private List<XSElementDeclWrapper> getChildElements(XSElementDecl element) {
        XSType type = element.getType();
        if (!type.isComplexType()) {
            return Collections.EMPTY_LIST;
        }
        List<XSElementDeclWrapper> rv = new ArrayList<XSElementDeclWrapper>();
        XSContentType xsContentType = type.asComplexType().getContentType();
        XSParticle particle = xsContentType.asParticle();
        if (particle != null) {
            XSTerm term = particle.getTerm();
            if (term.isModelGroup()) {
                XSModelGroup xsModelGroup = term.asModelGroup();
                XSParticle[] particles = xsModelGroup.getChildren();
                for (XSParticle p : particles) {
                    XSTerm pterm = p.getTerm();
                    if (pterm.isElementDecl()) { // xs:element inside complex type
                        XSElementDecl childElementDecl = (XSElementDecl) pterm;
                        XSElementDeclWrapper wrapper = new XSElementDeclWrapper(childElementDecl, p.getMinOccurs(), p.getMaxOccurs());
                        rv.add(wrapper);
                    }
                }
            }
        }
        return rv;
    }

    private static class XSElementDeclWrapper {
        private final XSElementDecl xsElementDecl;
        private final int minOccurs;
        private final int maxOccurs;

        public XSElementDeclWrapper(XSElementDecl xsElementDecl, int minOccurs, int maxOccurs) {
            this.xsElementDecl = xsElementDecl;
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
        }

        public XSElementDecl getXsElementDecl() {
            return xsElementDecl;
        }

        public int getMinOccurs() {
            return minOccurs;
        }

        public int getMaxOccurs() {
            return maxOccurs;
        }

    }

}
