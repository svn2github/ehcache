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

package net.sf.ehcache.config.generator.xsom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSSimpleType;

public abstract class XSDAttributeValueTypeFactory {

    public static XSDAttributeValueType createType(XSAttributeDecl attributeDecl) throws Exception {
        XSSimpleType type = attributeDecl.getType();
        if (type != null) {
            XSDAttributeValueType attributeValueType = doCreateType(type);
            attributeValueType.fillUpRestrictions(attributeDecl);
            return attributeValueType;
        }
        throw new Exception("Type is null for the attributeDecl: " + attributeDecl);
    }

    private static XSDAttributeValueType doCreateType(XSSimpleType type) throws Exception {
        XSRestrictionSimpleType restriction = type.asRestriction();
        List<String> enumeration = new ArrayList<String>();
        if (restriction != null) {
            Iterator<? extends XSFacet> i = restriction.getDeclaredFacets().iterator();
            while (i.hasNext()) {
                XSFacet facet = i.next();
                if (facet.getName().equals(XSFacet.FACET_ENUMERATION)) {
                    enumeration.add(facet.getValue().value);
                }
            }
        }
        if (enumeration != null && enumeration.size() > 0) {
            return new XSDAttributeValueType.XSDAttributeValueEnumerationType(enumeration.toArray(new String[0]));
        }
        if ("boolean".equalsIgnoreCase(type.getName())) {
            return new XSDAttributeValueType.XSDAttributeValueBooleanType();
        }
        if ("integer".equalsIgnoreCase(type.getName())) {
            return new XSDAttributeValueType.XSDAttributeValueIntegerType();
        }
        if ("anySimpleType".equalsIgnoreCase(type.getName())) {
            return new XSDAttributeValueType.XSDAttributeValueAnySimpleType();
        }
        if ("string".equalsIgnoreCase(type.getName())) {
            return new XSDAttributeValueType.XSDAttributeValueStringType();
        }
        if ("positiveInteger".equalsIgnoreCase(type.getName())) {
            return new XSDAttributeValueType.XSDAttributeValuePositiveIntegerType();
        }
        if ("nonNegativeInteger".equalsIgnoreCase(type.getName())) {
            return new XSDAttributeValueType.XSDAttributeValueNonNegativeIntegerType();
        }
        if ("memoryUnit".equalsIgnoreCase(type.getName())) {
            return new XSDAttributeValueType.XSDAttributeValueMemoryUnitType();
        }
        if ("memoryUnitOrPercentage".equalsIgnoreCase(type.getName())) {
            return new XSDAttributeValueType.XSDAttributeValueMemoryUnitOrPercentageType();
        }
        // if this exception is thrown, extend the above if clauses handling the type name with a new type in XSDAttributeValueType
        throw new Exception("Unknown type : " + (type == null ? "NULL" : type.getName())
                + ". Please handle this type following instructions in the source code");
    }

}
